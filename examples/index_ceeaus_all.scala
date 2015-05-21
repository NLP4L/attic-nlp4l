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
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import java.nio.file.FileSystems
import org.apache.lucene.index._
import org.apache.lucene.search.TermQuery
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core.analysis.AnalyzerBuilder
import org.nlp4l.core._

import scalax.file.Path
import scalax.file.PathSet

val index = "/tmp/index-ceeaus-all"

def lines(fl: Path, encoding: String): List[String] = {
  val is = new FileInputStream(fl.path)
  val r = new InputStreamReader(is, encoding)
  val br = new BufferedReader(r)
  var result: List[String] = Nil

  try{
    var line = br.readLine()
    while(line != null){
      result = result :+ line
      line = br.readLine()
    }
    result
  }
  finally{
    br.close
    r.close
    is.close
  }
}

def document(fl: Path, ja: Boolean): Document = {
  val ps: Array[String] = fl.path.split(File.separator)
  val file = ps(3)
  val typ = ps(2)
  val cat = "all"
  val encoding = if(ja) "sjis" else "UTF-8"
  val body = lines(fl, encoding)
  val body_set = if(ja) Set(Field("body_ja", body)) else Set(Field("body_en", body), Field("body_ws", body))
  Document(Set(
    Field("file", file), Field("type", typ), Field("cat", cat)) ++ body_set
  )
}

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// write documents into an index
val schema = SchemaLoader.loadFile("examples/schema/ceeaus.conf")
val writer = IWriter(index, schema)

val c: PathSet[Path] = Path("corpora", "CEEAUS", "PLAIN").children()
// write English docs
c.filter(e => e.name.indexOf("cjejus")<0 && e.name.endsWith(".txt")).foreach(g => writer.write(document(g, false)))
// write English docs
c.filter(e => e.name.indexOf("cjejus")>=0 && e.name.endsWith(".txt")).foreach(g => writer.write(document(g, true)))
writer.close

// search test
val searcher = ISearcher(index)
val results = searcher.search(query=new TermQuery(new Term("body_ja", "喫煙")), rows=10)

results.foreach(doc => {
  printf("[DocID] %d: %s\n", doc.docId, doc.get("file"))
})

// search test for ch4
val results2 = searcher.search(query=new TermQuery(new Term("body_ws", "still,")), rows=10)

results2.foreach(doc => {
  printf("[DocID] %d: %s\n", doc.docId, doc.get("file"))
})
