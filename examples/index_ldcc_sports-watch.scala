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
import org.apache.lucene.analysis.ja.{JapaneseAnalyzer, CompoundNounCnAnalyzer, CompoundNounRn2Analyzer, CompoundNounLn2Analyzer}
import org.apache.lucene.index._
import org.apache.lucene.search.TermQuery
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core._

import scalax.file.Path
import scalax.file.PathSet

val index = "/tmp/index-ldcc-sports-watch"

def document(file: Path): Document = {
  val ps: Array[String] = file.path.split(File.separator)
  // for Windows
  // val ps: Array[String] = file.path.split("\\\\")
  val cat = ps(3)
  val lines = file.lines().toArray
  val url = lines(0)
  val date = lines(1)
  val title = lines(2)
  val body = file.lines().drop(3).toList
  Document(Set(
    Field("url", url), Field("date", date), Field("cat", cat),
    Field("title", title), Field("body", body), Field("body_rn2", body), Field("body_ln2", body)
  ))
}

def teschema(): Schema = {
  val analyzerJa = Analyzer(new JapaneseAnalyzer)
  val analyzerCn = Analyzer(new CompoundNounCnAnalyzer)
  val analyzerRn2 = Analyzer(new CompoundNounRn2Analyzer)
  val analyzerLn2 = Analyzer(new CompoundNounLn2Analyzer)
  val fieldTypes = Map(
    "url" -> FieldType(null, true, true),
    "date" -> FieldType(null, true, true),
    "cat" -> FieldType(null, true, true),
    "title" -> FieldType(analyzerJa, true, true),
    "body" -> FieldType(analyzerCn, true, true, true, true),
    "body_rn2" -> FieldType(analyzerRn2, true, true, true, true),
    "body_ln2" -> FieldType(analyzerLn2, true, true, true, true)
  )
  Schema(analyzerJa, fieldTypes)
}

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// write documents into an index
val schema = teschema()
val writer = IWriter(index, schema)

val c: PathSet[Path] = Path("corpora", "ldcc", "text", "sports-watch").children()
c.filterNot( g => g.name.equals("LICENSE.txt") ).foreach( h => writer.write(document(h)) )

writer.close

// search
val searcher = ISearcher(index)
val results = searcher.search(query=new TermQuery(new Term("title", "iphone")), rows=10)

results.foreach(doc => {
  printf("[DocID] %d: %s\n", doc.docId, doc.get("title"))
})
