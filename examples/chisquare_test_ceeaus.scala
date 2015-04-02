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
import org.nlp4l.stats._

val index = "/tmp/index-ceeaus-all"

val schema = SchemaLoader.load("examples/schema/ceeaus.conf")
val reader = IReader(index, schema)

val docSetJUS = reader.subset(TermFilter("file", "ceejus_all.txt"))
val docSetNAS = reader.subset(TermFilter("file", "ceenas_all.txt"))

val totalCountJUS = WordCounts.totalCount(reader, "body_en", docSetJUS)
val totalCountNAS = WordCounts.totalCount(reader, "body_en", docSetNAS)

val words = List("i", "my", "me", "you", "your")

val wcJUS = WordCounts.count(reader, "body_en", words.toSet, docSetJUS)
val wcNAS = WordCounts.count(reader, "body_en", words.toSet, docSetNAS)

println("\t\tCEEJUS\tCEENAS\tchi square")
println("==============================================")
words.foreach{ w =>
  val countJUS = wcJUS.getOrElse(w, 0.toLong)
  val countNAS = wcNAS.getOrElse(w, 0.toLong)
  val cs = Stats.chiSquare(countJUS, totalCountJUS - countJUS, countNAS, totalCountNAS - countNAS, true)
  println("%8s\t%,6d\t%,6d\t%9.4f".format(w, countJUS, countNAS, cs))
}

reader.close
