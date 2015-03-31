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

import org.apache.lucene.util.PriorityQueue
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core.{IReader, Field, DocSet, RawReader}

import scala.collection.mutable

/**
 * Utility object for "counting the frequency of words" in the index with various criteria.
 */
object WordCounts {

  /**
   * Count frequencies of words in the index with the [[org.nlp4l.core.Schema]] in the given IReader.
   * @param reader the IReader instance
   * @param field the field name for counting words
   * @param words the set of words to be counted. All words will be returned if empty set is given.
   * @param docSet the index subset (set of document ids) for counting.
   * @param maxWords the max number of words to be returned. Top frequent words are returned if positive integer is given, otherwise all words will be returned. Default is -1 (all words will be returned.)
   * @return the Map of word and frequency associated this word.
   */
  def count(reader: IReader, field: String, words: Set[String], docSet: Set[Int], maxWords: Int = -1): Map[String, Long] =
    reader.getAnalyzer(field) match {
      case Some(analyzer) => count(reader, field, words, docSet, maxWords, analyzer)
      case _ => Map.empty[String, Long]
    }

  /**
   * Count frequencies of words in the index with given [[org.nlp4l.core.analysis.Analyzer]].
   * @param reader the RawReader instance
   * @param field the field name for counting words
   * @param words the set of words to be counted. All words will be returned if empty set is given.
   * @param docSet the index subset (set of document ids) for counting. Sums up word frequencies around whole index if empty set is given.
   * @param maxWords the max number of words to be returned. Top frequent words are returned if positive integer is given, otherwise all words will be returned. Default is -1 (all words will be returned.)
   * @param analyzer the Analyzer to re-analyze field values to count words. This is used when no term vector is available for given documents / field.
   * @return the Map of word and frequency associated this word.
   */
  def count(reader: RawReader, field: String, words: Set[String], docSet: Set[Int], maxWords: Int, analyzer: Analyzer): Map[String, Long] =
    if (docSet.isEmpty) countWholeIndex(reader, field, words, maxWords)
    else {
      val termsMap = mutable.Map.empty[String, Long]
      docSet.map(docId => reader.getTermVector(docId, field) match {
        case Some(tv) =>
          // count word frequencies from term vector.
          tv.terms.filter(t => if (words.isEmpty) true else words.contains(t._1))
            .foreach(t => termsMap += t._1 -> (termsMap.getOrElse(t._1, 0.toLong) + t._2.freq))
        case _ => reader.document(docId) match {
          // no term vector available. re-analyze field values and count terms.
          case Some(doc) =>
            doc.map.getOrElse(field, Field(field, ""))
              .values
              .foreach(analyzer.tokens(_).filter(token => if (words.isEmpty) true else words.contains(token.get("term").get))
              .foreach(token => termsMap += (token.get("term").get -> (termsMap.getOrElse(token.get("term").get, 0L) + 1L))))
          case _ => Unit
        }
      })

      if (maxWords > 0) {
        // only top N words and associated counts are needed
        val queue = new WordQueue(maxWords)
        termsMap.foreach(t => {
          queue.insertWithOverflow(t)
        })
        val builder = Map.newBuilder[String, Long]
        for (i <- (0 to queue.size() - 1))
          builder += queue.pop()
        builder.result()
      } else {
        // all words and associated counts are needed
        termsMap.toMap
      }
    }

  private def countWholeIndex(reader: RawReader, field: String, words: Set[String], maxWords: Int): Map[String, Long] = {
    if (maxWords > 0) reader.topTermsByTotalTermFreq(field, maxWords).map(e => (e._1, e._3)).toMap
    else reader.field(field) match {
      // enumerate all terms for this field to sum up term frequencies
      case Some(f) => f.terms.
        filter(t => if (words.isEmpty) true else words.contains(t.text)).
        map(t => (t.text, t.totalTermFreq)).toMap
      case _ => Map.empty[String, Long]
    }
  }

  /**
   * Count document frequencies of words in the whole index.
   * @param reader the IReder instance
   * @param field the field name for counting words
   * @param words the set of words to be counted. All words will be returned if empty set is given.
   * @param maxWords the max number of words to be returned. Top frequent words are returned if positive integer is given, otherwise all words will be returned. Default is -1 (all words will be returned.)
   * @return
   */
  def countDF(reader: RawReader, field: String, words: Set[String], maxWords: Int = -1): Map[String, Long] = {
    if (maxWords > 0) reader.topTermsByDocFreq(field, maxWords).map(e => (e._1, e._3)).toMap
    else reader.field(field) match {
      // enumerate all terms for this field to sum up term document frequencies
      case Some(f) => f.terms.
        filter(t => if (words.isEmpty) true else words.contains(t.text)).
        map(t => (t.text, t.docFreq.toLong)).toMap
      case _ => Map.empty[String, Long]
    }
  }

  def countPrefix(reader: RawReader, field: String, prefix: String): Long = {
    RawWordCounts.countPrefix(reader.ir, field, prefix)
  }

  /**
   * Count the frequency for all words in the index with the [[org.nlp4l.core.Schema]] in the given IReader.
   * @param reader the IReader instance
   * @param field the field name for counting words
   * @param docSet the set of words to be counted. All words will be returned if empty set is given.
   * @return the Map of word and frequency associated this word.
   */
  def totalCount(reader: IReader, field: String, docSet: Set[Int]): Long = {
    count(reader, field, Set.empty[String], docSet).values.sum
  }

  /**
   * Count the frequency for all words in the index with given [[org.nlp4l.core.analysis.Analyzer]].
   * @param reader the RawReader instance
   * @param field the field name for counting words
   * @param docSet the set of words to be counted. All words will be returned if empty set is given.
   * @param analyzer the Analyzer to re-analyze field values to count words. This is used when no term vector is available for given documents / field.
   * @return the Map of word and frequency associated this word.
   */
  def totalCount(reader: RawReader, field: String, docSet: Set[Int], analyzer: Analyzer): Long = {
    count(reader, field, Set.empty[String], docSet, -1, analyzer).values.sum
  }
}

/**
 * Class representing priority queue. (for internal uses)
 * @param size the queue size
 */
class WordQueue(size: Int) extends PriorityQueue[(String, Long)](size) {
  override def lessThan(w1: (String, Long), w2: (String, Long)): Boolean = w1._2 < w2._2
}
