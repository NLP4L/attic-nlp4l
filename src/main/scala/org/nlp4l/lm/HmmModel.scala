/*
 * Copyright 2015 RONDHUIT Co.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nlp4l.lm

import org.nlp4l.core._
import org.nlp4l.core.analysis.{Analyzer, AnalyzerBuilder}

trait HmmModelSchema {
  def schema(): Schema = {
    val analyzer = Analyzer(new org.apache.lucene.analysis.core.WhitespaceAnalyzer)
    val builder = AnalyzerBuilder()
    builder.withTokenizer("whitespace")
    builder.addTokenFilter("shingle", "minShingleSize", "2", "maxShingleSize", "2", "outputUnigrams", "false")
    val analyzer2g = builder.build
    val fieldTypes = Map(
      "begin" -> FieldType(analyzer, true, true, true, true),
      "class" -> FieldType(analyzer, true, true, true, true),
      "class_2g" -> FieldType(analyzer2g, true, true, true, true),
      "word_class" -> FieldType(analyzer, true, true, true, true),
      "word" -> FieldType(analyzer, true, true, true, true)
    )
    val analyzerDefault = analyzer
    Schema(analyzerDefault, fieldTypes)
  }
}

class HmmModelIndexer(index: String) extends HmmModelSchema {

  // write documents into an index
  val writer = IWriter(index, schema)

  def addDocument(doc: Seq[(String, String)]): Unit = {
    val fBegin = Field("begin", doc.head._2)
    val fClass = Field("class", doc.map(_._2).mkString(" "))
    val fClass2g = Field("class_2g", doc.map(_._2).mkString(" "))
    val fWordClass = Field("word_class", doc.map(e => "%s_%s".format(e._1, e._2)).mkString(" "))
    val fWord = Field("word", doc.map(_._1).mkString(" "))
    writer.write(Document(Set(
      fBegin, fClass, fClass2g, fWordClass, fWord
    )))
  }

  def close(): Unit = {
    writer.close
  }
}

object HmmModelIndexer {
  def apply(index: String) = new HmmModelIndexer(index)
}

class HmmModel(index: String) extends HmmModelSchema {
  val UNKNOWN_WORD = -1
  val UNKNOWN_CLASS = -1
  val UNKNOWN_CLASS_LABEL = "X"
  val reader = IReader(index, schema)

  // create class# -> class name dictionary
  private val classes = reader.field("class").get.terms.map(e => (e.text, e.totalTermFreq.toDouble)).toArray

  // create class name -> class# dictionary
  val classNamesDic = (for{
    i <- 0 to classes.size - 1
    p = Pair(classes(i)._1, i)
  } yield p).toMap

  // create initial state costs dictionary and connection costs table
  val SMALL_PROB = 0.0000001.toDouble
  val MAX_COST = cost(SMALL_PROB)
  val beginClasses = reader.field("begin").get.terms.map(e => (e.text, e.totalTermFreq.toDouble)).toMap
  val totalBeginCount = reader.sumTotalTermFreq("begin").toDouble
  val costInitialState = new Array[Int](classes.size)
  val costConnection = new Array[Array[Int]](classes.size)
  for(i <- 0 to classes.size - 1){
    costConnection(i) = new Array[Int](classes.size)
    for(j <- 0 to classes.size - 1){
      costConnection(i)(j) = MAX_COST
    }
  }

  // compute P( C_1 | C_0 ) = Count( beginClass ) / totalBeginCount
  for(i <- 0 to classes.size - 1){
    val c = classes(i)._1
    val prob = beginClasses.getOrElse(c, SMALL_PROB) / totalBeginCount
    costInitialState(i) = cost(prob)
  }

  // compute P( C_n | C_m ) = Count( C_m C_n ) / Count( C_m )
  for(i <- reader.field("class_2g").get.terms.map(e => (e.text, e.totalTermFreq.toDouble))){
    val cns = i._1.split(" ")
    val idx0 = classNamesDic.get(cns(0)).get
    val idx1 = classNamesDic.get(cns(1)).get
    val prob = i._2 / classes(idx0)._2
    costConnection(idx0)(idx1) = cost(prob)
  }

  // create word dictionary
  val words = reader.field("word").get.terms.map(e => (e.text, e.totalTermFreq.toDouble)).toArray

  // create word -> word# dictionary
  val wordDic = (for{
    i <- 0 to words.size - 1
    p = Pair(words(i)._1, i)
  } yield p).toMap

  // compute P( W_n | C_n ) = Count( C_n, W_n ) / Count( C_n )
  val wordClasses = reader.field("word_class").get.terms.map(e => (e.text, e.totalTermFreq.toDouble)).toList
  val tempDic = createDictionary(List.empty[(String, List[(Int, Int)])], List.empty[(Int, Int)], "", wordClasses)

  // create FST
  val fst = SimpleFST(true)
  private val costConditionalClasses = new Array[List[(Int,Int)]](words.size)
  addWord(fst, 0, tempDic, costConditionalClasses)
  fst.finish

  reader.close

  def cost(probability: Double): Int = {
    (scala.math.log10(probability) * (-1000.toDouble)).toInt
  }

  def createDictionary(result: List[(String, List[(Int, Int)])], value: List[(Int, Int)], word: String, list: List[(String, Double)]):
    List[(String, List[(Int, Int)])] = {
    if(list.isEmpty){
      if(!value.isEmpty) result :+ (word, value)
      else result
    }
    else{
      val h = list.head
      println(h)
      val wcs = h._1.split("_")
      val w1 = wcs(0)
      val idx0 = wordDic.get(w1).get
      val idx1 = classNamesDic.get(wcs(1)).get
      val prob = h._2 / words(idx0)._2
      if(w1 == word){
        createDictionary(result, value :+ (idx1, cost(prob)), word, list.tail)
      }
      else{
        if(!value.isEmpty) createDictionary(result :+ (word, value), List((idx1, cost(prob))), w1, list.tail)
        else createDictionary(result, List((idx1, cost(prob))), w1, list.tail)
      }
    }
  }

  def connectionCost(leftClass: Int, rightClass: Int): Int = {
    if(leftClass < 0 || rightClass < 0) MAX_COST
    else{
      costConnection(leftClass)(rightClass)
    }
  }

  private def addWord(fst: SimpleFST, index: Int, entries: List[(String, List[(Int, Int)])], costConditionalClasses: Array[List[(Int,Int)]]): Unit = {
    if(!entries.isEmpty){
      val entry = entries.head
      fst.addEntry(entry._1, index)
      costConditionalClasses(index) = entry._2
      addWord(fst, index + 1, entries.tail, costConditionalClasses)
    }
  }

  def className(idx: Int): String = idx match {
    case UNKNOWN_CLASS => {
      UNKNOWN_CLASS_LABEL
    }
    case _ => {
      classes(idx)._1
    }
  }

  def classFreq(idx: Int): Int = classes(idx)._2.toInt

  def classNum(): Int = classes.size

  def conditionalClassesCost(idx: Int): List[(Int,Int)] =
    idx match {
      case UNKNOWN_WORD => {
        List((UNKNOWN_CLASS, MAX_COST))
      }
      case _ => {
        costConditionalClasses(idx)
      }
    }
}

object HmmModel {
  def apply(index: String) = new HmmModel(index)
}
