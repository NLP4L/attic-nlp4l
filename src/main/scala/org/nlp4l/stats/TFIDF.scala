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

import org.nlp4l.core.IReader

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
  def tfVector(reader: IReader, field: String, docId: Int, words: Set[String] = Set.empty): (Vector[String], Vector[Long]) = {
    if (words.isEmpty) {
      val tMap = TreeMap(WordCounts.count(reader, field, words, Set(docId)).toArray: _*)
      (tMap.keys.toVector, tMap.values.toVector)
    } else {
      val countMap = WordCounts.count(reader, field, words, Set(docId))
      val features = words.toSeq.sorted
      val vector = features.map(w => countMap.getOrElse(w, 0L)).toVector
      (features.toVector, vector)
    }
  }

  /**
   * Generate simple tf based feature vector from specified documents.
   * @param reader the IReader instance
   * @param field the field name for counting words
   * @param docIds  the list of Lucene document id
   * @param words the set of words(terms) considered as feature. All words(terms) will be taken as features if empty set is given.
   * @return the Vector of words and the feature vectors
   */
  def tfVectors(reader: IReader, field: String, docIds: List[Int], words: Set[String] = Set.empty): (Vector[String], List[Vector[Long]]) = {
    val countMaps = docIds.map(id => WordCounts.count(reader, field, words, Set(id)))
    val mergedWords = if (words.isEmpty) countMaps.flatMap(_.keys).toSet.toSeq.sorted else words.toSeq.sorted
    val vectors = countMaps.map(map => mergedWords.map(w => map.getOrElse(w, 0L)).toVector)
    (mergedWords.toVector, vectors)
  }
}
