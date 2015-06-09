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

package org.nlp4l.util

import org.nlp4l.core.RawReader
import org.nlp4l.stats.WordCounts

trait FeatureSelector {

  def parseCriteriaOption(parsed: Map[Symbol, String], list: List[String]): Map[Symbol, String] = list match {
    case Nil => parsed
    case "--maxDFPercent" :: value :: tail => parseCriteriaOption(parsed + ('maxDFPercent -> value), tail)
    case "--minDF" :: value :: tail => parseCriteriaOption(parsed + ('minDF -> value), tail)
    case "--maxFeatures" :: value :: tail => parseCriteriaOption(parsed + ('maxFeatures -> value), tail)
    case value :: tail => parseCriteriaOption(parsed, tail)
  }

  def selectFeatures(reader: RawReader, field: String, minDF: Int = 1, maxDFPercent: Double = 1.0, maxNumTerms: Int = -1): Set[String] = {
    val docFreqs = WordCounts.countDF(reader, field, Set.empty[String])
    val numDocs = reader.numDocs.toDouble
    val wordsFilteredByDF = docFreqs.filter(_._2 >= minDF).filter(_._2 / numDocs <= maxDFPercent).map(_._1).toSet
    if (maxNumTerms > 0)
      reader.topTermsByDocFreq(field, maxNumTerms).map(_._1).toSet & wordsFilteredByDF
    else
      wordsFilteredByDF
  }
}
