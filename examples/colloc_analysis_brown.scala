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
import org.apache.lucene.analysis.standard.StandardTokenizerFactory
import org.nlp4l.colloc._
import scala.io._
import scalax.file.Path
import scalax.file.PathSet

val index = "/tmp/index-brown-colloc"

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// create Collocational Analysis model index
val c: PathSet[Path] = Path("corpora", "brown", "brown").children()
val indexer = CollocationalAnalysisModelIndexer(index, new StandardTokenizerFactory(new java.util.HashMap[String, String]()))
c.filter{ e =>
  val s = e.name
  val c = s.charAt(s.length - 1)
  c >= '0' && c <= '9'
}.toList.sorted.foreach{ f =>
  val source = Source.fromFile(f.path, "UTF-8")
  source.getLines().map(_.trim).filter(_.length > 0).foreach { g =>
    val pairs = g.split("\\s+")
    val doc = pairs.map{h => h.split("/")}.filter{_.length==2}.map(_(0)).mkString(" ")
    indexer.addDocument(doc)
  }
}

indexer.close()

// read the model index
val model = CollocationalAnalysisModel(index)

val WORD = "found"
println("\n=== print surrounding of the word %s ===".format(WORD))
val result = model.collocationalWordsStats(WORD, 10)

def arrangedString(data: Seq[Seq[(String, Long)]], i: Int, pos: Int): String = {
  if(data.size > i && data(i).size > pos) "%10s(%2d)".format(data(i)(pos)._1, data(i)(pos)._2) else ""
}

for(i <- 0 to 9){
  println("|%14s|%14s|%14s|%10s|%14s|%14s|%14s|".format(
    arrangedString(result, 5, i), arrangedString(result, 4, i), arrangedString(result, 3, i),
    if(i == 0) WORD else "",
    arrangedString(result, 0, i), arrangedString(result, 1, i), arrangedString(result, 2, i)
  ))
}
