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

val index = "/tmp/index-brown"

val schema = SchemaLoader.load("examples/schema/brown.conf")
val reader = IReader(index, schema)

val docSetROM = reader.subset(TermFilter("cat", "romance"))
val docSetNEW = reader.subset(TermFilter("cat", "news"))

val totalCountROM = WordCounts.totalCount(reader, "body", docSetROM)
val totalCountNEW = WordCounts.totalCount(reader, "body", docSetNEW)

val words = List("could", "will")

val wcROM = WordCounts.count(reader, "body", words.toSet, docSetROM)
val wcNEW = WordCounts.count(reader, "body", words.toSet, docSetNEW)

println("\t\tromance\tnews\tchi square")
println("==============================================")
words.foreach{ w =>
  val countROM = wcROM.getOrElse(w, 0.toLong)
  val countNEW = wcNEW.getOrElse(w, 0.toLong)
  val cs = Stats.chiSquare(countROM, totalCountROM - countROM, countNEW, totalCountNEW - countNEW, true)
  println("%8s\t%,6d\t%,6d\t%9.4f".format(w, countROM, countNEW, cs))
}

reader.close
