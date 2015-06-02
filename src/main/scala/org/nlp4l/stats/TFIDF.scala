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

package org.nlp4l.stats

import org.nlp4l.core.{RawReader, IReader}

import scala.collection.immutable.TreeMap

/**
 * Utility object to generate feature vectors representing documents/corpus weighted by tf-idf.
 */
object TFIDF {

  /**
   * Generate simple tf based feature vector from a document.
   * @param reader the IReader instance
   * @param field the field name for counting words
   * @param docId the Lucene document id
   * @param words the set of words(terms) considered as feature. All words(terms) will be taken as features if empty set is given.
   * @return the Vector of words and the feature vector
   */
  def tfVector(reader: IReader, field: String, docId: Int, words: Set[String] = Set.empty, tfMode: String = "n", termBoosts: Map[String, Double] = Map.empty): (Seq[String], Seq[Long]) = {
    val (features, vector) =  tfIdfVector(reader, field, docId, words, tfMode, idfMode="n", termBoosts=termBoosts)
    (features, vector.map(_.toLong))
  }

  /**
   * Generate simple tf based feature vector from specified documents.
   * @param reader the IReader instance
   * @param field the field name for counting words
   * @param docIds  the list of Lucene document id
   * @param words the set of words(terms) considered as feature. All words(terms) will be taken as features if empty set is given.
   * @return the pair of words and the feature vectors
   */
  def tfVectors(reader: IReader, field: String, docIds: List[Int], words: Set[String] = Set.empty, tfMode: String = "n", termBoosts: Map[String, Double] = Map.empty): (Seq[String], Stream[Seq[Long]]) = {
    val (features, vectors) = tfIdfVectors(reader, field, docIds, words, tfMode, idfMode="n", termBoosts=termBoosts)
    (features, vectors.map(_.map(_.toLong)))
  }

  /**
   * Generate tf-idf based feature vector from a document.
   *
   * tf and idf calculations can be varied according to "tfMode" and "idfMode" parameters.
   * See http://nlp.stanford.edu/IR-book/html/htmledition/variant-tf-idf-functions-1.html for theoretical backgrounds.
   *
   * In default, when tfMode and idfMode are not given, weight for each term is given by this basic tf-idf formula
   *
   *  (tf) * log(N / df)
   *
   * where tf is term frequency of the term in given document, N is the total number of documents and df is document frequency for the term.
   *
   * @param reader the IReader instance
   * @param field the field name for counting words
   * @param docId the Lucene document id
   * @param words the set of words(terms) considered as feature. All words(terms) will be taken as features if empty set is given.
   * @param tfMode tf calculation mode. Expected values are "n" (normal), "l" (logarithm), "m" (maximum normalization), "b" (boolean), "L" (Log ave), "w" (sublinear weighted). The default value is "n"
   * @param a the smoothing term for tfMode "m". The default value is 0.4.
   * @param idfMode idf calculation mode. Expected values are "n" (no), "t" (idf), "p" (prob idf). The default value is "t"
   * @return the Vector of words and the feature vector
   */
  def tfIdfVector(reader: IReader, field: String, docId: Int, words: Set[String] = Set.empty, tfMode: String = "n", a: Double = 0.4, idfMode: String = "t", termBoosts: Map[String, Double] = Map.empty): (Seq[String], Seq[Double]) = {
    val numDocs = Some(reader.numDocs)
    val maxTF = if (tfMode == "m") Some(reader.topTermsByTotalTermFreq(field, 1)(0)._3) else None
    val countMap = WordCounts.count(reader, field, words, Set(docId))
    val aveTF = if (tfMode == "L") Some(Stats.average(countMap.values)) else None
    if (words.isEmpty) {
      val dfMap = WordCounts.countDF(reader, field, countMap.keys.toSet)
      val tfIdfMap = countMap.map(e => (e._1, tf(e._2, tfMode, maxTF, a, aveTF) * termBoosts.getOrElse(e._1, 1.0) * idf(dfMap(e._1), idfMode, numDocs)))
      val tMap = TreeMap(tfIdfMap.toArray: _*)
      (tMap.keys.toVector, tMap.values.toVector)
    } else {
      val dfMap = WordCounts.countDF(reader, field, words)
      val features = words.toSeq.sorted
      val vector = features.map(w => if (countMap.contains(w)) tf(countMap(w), tfMode, maxTF, a, aveTF) * termBoosts.getOrElse(w, 1.0) * idf(dfMap(w), idfMode, numDocs) else 0.0).toVector
      (features.toVector, vector)
    }
  }

  /**
   * Generate tf-idf based feature vector from a document.
   *
   * See also documentation for tfIdfVector().
   *
   * @param reader the IReader instance
   * @param field the field name for counting words
   * @param docIds the list of Lucene document id
   * @param words the set of words(terms) considered as feature. All words(terms) will be taken as features if empty set is given.
   * @param tfMode tf calculation mode. The default value is "n"
   * @param a the smoothing term for tfMode "m". The default value is 0.4.
   * @param idfMode idf calculation mode. The default value is "t"
   * @return the pair of words and the feature vectors
   */
  def tfIdfVectors(reader: IReader, field: String, docIds: List[Int], words: Set[String] = Set.empty, tfMode: String = "n", a: Double = 0.4, idfMode: String = "t", termBoosts: Map[String, Double] = Map.empty): (Seq[String], Stream[Seq[Double]]) = {
    val numDocs = Some(reader.numDocs)
    val maxTF = if (tfMode == "m") Some(reader.topTermsByTotalTermFreq(field, 1)(0)._3) else None
    val countMaps = docIds.map(id => WordCounts.count(reader, field, words, Set(id)))
    val dfMap = if (words.isEmpty) WordCounts.countDF(reader, field, countMaps.flatMap(_.keys).toSet) else WordCounts.countDF(reader, field, words)
    val mergedWords = if (words.isEmpty) countMaps.flatMap(_.keys).toSet.toSeq.sorted else words.toSeq.sorted
    def vectors(countMaps: List[Map[String, Long]]): Stream[Seq[Double]] = countMaps match {
      case Nil => Stream.empty
      case map :: tail => {
        val aveTF = if (tfMode == "L") Some(Stats.average(map.values)) else None
        def vec = mergedWords.view.map(w => if (map.contains(w)) tf(map(w), tfMode, maxTF, a, aveTF) * termBoosts.getOrElse(w, 1.0) * idf(dfMap(w), idfMode, numDocs) else 0.0)
        vec #:: vectors(tail)
      }
    }
    (mergedWords.toVector, vectors(countMaps))
  }

  /**
   * Calculate TF variants.
   *
   * @param v the natural TF value for the term
   * @param mode the notation for each variant
   * @param maxTF the max term frequency in the whole documents. Required when "m" is given to mode.
   * @param a the smoothing term for tfMode "m".
   * @param aveTF the average of TF in the document. Required when "L" is given to mode.
   * @return the TF value
   */
  private def tf(v: Long, mode: String, maxTF: Option[Long] = None, a: Double = 0.4, aveTF: Option[Double] = None): Double = mode match {
    // normal
    case "n" => v.toDouble
    // logarithm
    case "l" => 1 + math.log(v)
    // maximum tf normalization
    case "m" => {
      if (a < 0.0 || a > 1.0) throw new IllegalArgumentException("a parameter must be in between 0.0 to 1.0")
      maxTF match {
        case Some(value) => a + (a * v) / value.toDouble
        case _ => throw new IllegalArgumentException("maxTF parameter is required.")
      }
    }
    // boolean
    case "b" => if (v > 0) 1.0 else 0.0
    // Log ave
    case "L" => aveTF match {
      case Some(value) => (1 + math.log(v)) / (1 + math.log(value))
      case _ => throw new IllegalArgumentException("aveTF parameter is required.")
    }
    // sublinear weighted tf
    case "w" => if (v > 0) 1 + math.log(v) else 0.0
    // unknown
    case _ => throw new IllegalArgumentException("Unknown notation: " + mode)
  }

  /**
   * Calculate IDF variants.
   *
   * @param v the document frequency for the term
   * @param mode the notation for each variant
   * @param numDocs the total number of documents. Required when "t" or "p" is given to notation.
   * @return the IDF value
   */
  private def idf(v: Long, mode: String, numDocs: Option[Int] = None): Double = mode match {
    // no
    case "n" => 1
    // idf
    case "t" => numDocs match {
      case Some(value) => math.log(value / v.toDouble)
      case _ => throw new IllegalThreadStateException("numDocs parameter is required.")
    }
    // prob idf
    case "p" => numDocs match {
      case Some(value) => math.max(0, math.log((value - v) / v.toDouble))
      case _ => throw new IllegalThreadStateException("numDocs parameter is required.")
    }
    // unknown
    case _ => throw new IllegalArgumentException("Unknown notation: " + mode)
  }
}
