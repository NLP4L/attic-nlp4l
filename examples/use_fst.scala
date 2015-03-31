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

val index = "/tmp/index-brown"

// load schema from file
val schema = SchemaLoader.load("examples/schema/brown.conf")

val reader = IReader(index, schema)
val fst = SimpleFST()

reader.field("body_pos").get.terms.foreach { term =>
  fst.addEntry(term.text, term.totalTermFreq)
}

fst.finish

val STR = "iaminnewyork"

for(pos <- 0 to STR.length - 1){
  fst.leftMostSubstring(STR, pos).foreach { e =>
    print("%s".format("             ".substring(0, pos)))
    println("%s => %d".format(STR.substring(pos, e._1), e._2))
  }
}
