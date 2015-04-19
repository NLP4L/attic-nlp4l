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

import java.io.File
import java.nio.file.FileSystems
import org.apache.lucene.index._
import org.apache.lucene.search.TermQuery
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core._
import org.nlp4l.lm.{HmmTagger, HmmModel, HmmModelIndexer}
import scala.io._
import scala.util.matching.Regex
import scalax.file.Path
import scalax.file.PathSet

val index = "/tmp/index-brown-hmm"

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// create HMM model index
val c: PathSet[Path] = Path("corpora", "brown", "brown").children()
val indexer = HmmModelIndexer(index)
c.filter{ e =>
  val s = e.name
  val c = s.charAt(s.length - 1)
  c >= '0' && c <= '9'
}.foreach{ f =>
  val source = Source.fromFile(f.path, "UTF-8")
  source.getLines().map(_.trim).filter(_.length > 0).foreach { g =>
    val pairs = g.split("\\s+")
    val doc = pairs.map{h => h.split("/")}.filter{_.length==2}.map{i => (i(0).toLowerCase(), i(1))}
    indexer.addDocument(doc)
  }
}

indexer.close()

// read the model index
val model = HmmModel(index)

// print the model
println("\n=== classes ===")
val tableSize = model.classNum
for(i <- 0 to tableSize - 1){
  println(model.className(i))
}

println("\n=== classNamesDic ===")
model.classNamesDic.foreach{
  println(_)
}

println("\n=== costInitialState ===")
model.costInitialState.foreach{
  println(_)
}

println("\n=== costConnection ===")
print("   ")
for(i <- 0 to tableSize - 1){
  print("%10s".format(model.className(i)))
}
println
for(i <- 0 to tableSize - 1){
  print("%4s ".format(model.className(i)))
  for(j <- 0 to tableSize - 1){
    print("%8d  ".format(model.costConnection(i)(j)))
  }
  println
}

println("\n=== words ===")
model.words.foreach{
  println(_)
}

println("\n=== wordDic ===")
model.wordDic.foreach{
  println(_)
}

println("\n=== wordClasses ===")
model.wordClasses.foreach{
  println(_)
}

println("\n=== wordDic(tempDic) ===")
model.tempDic.foreach{ e =>
  println(e._1)
  e._2.foreach{ g =>
    println("\t%s %d (%d) %d".format(model.className(g._1), model.classFreq(g._1), g._1, g._2))
  }
}

println("\n=== tagger test ===")
val tagger = HmmTagger(model)

tagger.tokens("I like to go to France .")
