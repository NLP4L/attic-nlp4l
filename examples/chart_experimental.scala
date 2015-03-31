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

import org.nlp4l.core._
import org.nlp4l.core.analysis._
import org.nlp4l.gui._
import org.nlp4l.stats.WordCounts

val index = "/tmp/index-reuters"

val reader = RawReader(index)

val analyzer = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer(null.asInstanceOf[org.apache.lucene.analysis.util.CharArraySet]))

val usDS = reader.subset(TermFilter("places", "usa"))
val jpDS = reader.subset(TermFilter("places", "japan"))

val usMap = WordCounts.count(reader, "body", Set("war", "peace"), usDS, -1, analyzer)
val jpMap = WordCounts.count(reader, "body", Set("war", "peace"), jpDS, -1, analyzer)

val presentation = BarChart(List(("US",usMap), ("Japan",jpMap)))

val server = new SimpleHttpServer(presentation)

// launch web server
// access http://localhost:6574/shutdown in order to shutdown the web server
server.service
