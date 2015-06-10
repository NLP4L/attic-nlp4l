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

import scalax.file.Path
import scalax.file.PathSet

val srcIndex = "/tmp/index-ldcc"
val index = "/tmp/index-ldcc-part"

val searcher = ISearcher(srcIndex)

// write documents into an index
val schema = SchemaLoader.loadFile("examples/schema/ldcc.conf")
val writer = IWriter(index, schema)

def writeCategoryDocs(cat: String): Unit = {
  val results = searcher.search(query=new TermQuery(new Term("cat", cat)), rows=1000)
  results.foreach(doc => {
    writer.write(doc)
  })
}

writeCategoryDocs("dokujo-tsushin")
writeCategoryDocs("sports-watch")

writer.close
