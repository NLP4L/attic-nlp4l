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
import java.util
import org.apache.lucene.analysis.ja.JapaneseTokenizerFactory
import org.nlp4l.colloc._
import scalax.file.Path
import scalax.file.PathSet

val index = "/tmp/index-ldcc-colloc"

def document(file: Path): String = {
  val ps: Array[String] = file.path.split("/")
  val lines = file.lines().toArray
  file.lines().drop(3).toList.mkString
}

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// create Collocational Analysis model index
val indexer = CollocationalAnalysisModelIndexer(index, new JapaneseTokenizerFactory(new util.HashMap[String, String]()))
val c: PathSet[Path] = Path("corpora", "ldcc", "text", "it-life-hack").children()
c.filterNot( g => g.name.equals("LICENSE.txt") ).take(100).    // TODO: remove take(100) (it is inserted to avoid OOME)
  foreach( h => indexer.addDocument(document(h)) )

indexer.close()

// read the model index
val model = CollocationalAnalysisModel(index)

val WORD = "高"
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
