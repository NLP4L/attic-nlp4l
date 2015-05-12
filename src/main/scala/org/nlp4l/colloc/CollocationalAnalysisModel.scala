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

package org.nlp4l.colloc

import org.apache.lucene.analysis.core.{BothEndsFilter, LowerCaseFilter}
import org.apache.lucene.analysis.shingle.ShingleFilter
import org.apache.lucene.analysis.util.TokenizerFactory
import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.analysis.{Analyzer => LuceneAnalyzer}
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.util.PriorityQueue
import org.nlp4l.core._
import org.nlp4l.core.analysis.Analyzer

trait CollocationalAnalysisModelSchema {
  def schema(tokenizerFactory: TokenizerFactory): Schema = {
    val fieldTypes = Map(
      "word" -> FieldType(analyzer(tokenizerFactory), true, true, true, true),
      "word1" -> FieldType(analyzer(tokenizerFactory, 2), true, true, true, true),
      "word2" -> FieldType(analyzer(tokenizerFactory, 3), true, true, true, true),
      "word3" -> FieldType(analyzer(tokenizerFactory, 4), true, true, true, true),
      "word1r" -> FieldType(analyzer(tokenizerFactory, 2, true), true, true, true, true),
      "word2r" -> FieldType(analyzer(tokenizerFactory, 3, true), true, true, true, true),
      "word3r" -> FieldType(analyzer(tokenizerFactory, 4, true), true, true, true, true)
    )
    val analyzerDefault = analyzer(tokenizerFactory)
    Schema(analyzerDefault, fieldTypes)
  }

  private def analyzer(tokenizerFactory: TokenizerFactory, size: Int = 1, reverse: Boolean = false): Analyzer = {
    Analyzer(new BothEndsAnalyzer(tokenizerFactory, size, reverse))
  }

  class BothEndsAnalyzer(tokenizerFactory: TokenizerFactory, size: Int, reverse: Boolean) extends LuceneAnalyzer {
    override protected def createComponents(fieldName: String): TokenStreamComponents = {
      val tokenizer: Tokenizer = tokenizerFactory.create()
      if(size > 1){
        val sf: ShingleFilter = new ShingleFilter(tokenizer, size, size)
        sf.setOutputUnigrams(false)
        val lf: LowerCaseFilter = new LowerCaseFilter(sf)
        new TokenStreamComponents(tokenizer, new BothEndsFilter(lf, reverse))
      }
      else{
        val lf: LowerCaseFilter = new LowerCaseFilter(tokenizer)
        new TokenStreamComponents(tokenizer, lf)
      }
    }
  }
}

class CollocationalAnalysisModelIndexer(index: String, tokenizerFactory: TokenizerFactory) extends CollocationalAnalysisModelSchema {

  // write documents into an index
  val writer = IWriter(index, schema(tokenizerFactory))

  def addDocument(text: String): Unit = {
    writer.write(Document(Set(
      Field("word", text),
      Field("word1", text), Field("word2", text), Field("word3", text),
      Field("word1r", text), Field("word2r", text), Field("word3r", text)
    )))
  }

  def close(): Unit = {
    writer.close
  }
}

object CollocationalAnalysisModelIndexer {
  def apply(index: String, tokenizerFactory: TokenizerFactory) = new CollocationalAnalysisModelIndexer(index, tokenizerFactory)
}

class CollocationalAnalysisModel(index: String) extends CollocationalAnalysisModelSchema {

  val TOKEN_SEPARATOR: String = " "
  val reader = RawReader(index)

  def collocationalWordsStats(word: String, max: Int): Array[Seq[(String, Long)]] = {
    val result: Array[Seq[(String, Long)]] = new Array[Seq[(String, Long)]](6)
    result(0) = wordsStartWith("word1", word, max)
    result(1) = wordsStartWith("word2", word, max)
    result(2) = wordsStartWith("word3", word, max)
    result(3) = wordsStartWith("word1r", word, max)
    result(4) = wordsStartWith("word2r", word, max)
    result(5) = wordsStartWith("word3r", word, max)
    result
  }

  private def wordsStartWith(field: String, word: String, max: Int): Seq[(String, Long)] = {
    val target = word + TOKEN_SEPARATOR
    // TODO: this leads an OutOfMemoryError
    val collocs = reader.field(field).get.terms.dropWhile(!_.text.startsWith(target)).takeWhile(_.text.startsWith(target)).
      map(a => (a.text.split(TOKEN_SEPARATOR)(1), a.totalTermFreq))
    val queue = new WordCountQueue(max)
    collocs.foreach(queue.insertWithOverflow(_))
    //println("field=%s, word='%s', target='%s', queue.size=%d".format(field, word, target, queue.size))
    val result = new Array[(String, Long)](queue.size)
    for(i <- (0 to queue.size() - 1).reverse){
      result(i) = queue.pop()
    }
    result
  }

  def close(): Unit = {
    reader.close
  }

  class WordCountQueue(size: Int) extends PriorityQueue[(String, Long)](size) {
    override def lessThan(w1: (String, Long), w2: (String, Long)): Boolean =
      if(w1._2 == w2._2) w1._1 > w2._1 else w1._2 < w2._2
  }
}

object CollocationalAnalysisModel {
  def apply(index: String) = new CollocationalAnalysisModel(index)
}
