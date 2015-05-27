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

import collection.JavaConversions._
import it.cnr.isti.hpc.io.reader.JsonRecordParser
import it.cnr.isti.hpc.io.reader.RecordReader
import it.cnr.isti.hpc.wikipedia.article.Article
import it.cnr.isti.hpc.wikipedia.article.Link
import it.cnr.isti.hpc.wikipedia.reader.filter.TypeFilter
import org.nlp4l.core._
import org.nlp4l.core.analysis.Analyzer
import scala.util.matching.Regex

val index = "/tmp/index-jawiki"

val schema = SchemaLoader.loadFile("examples/schema/jawiki.conf")
val writer = IWriter(index, schema)

def addDocument(id: Int, title: String, body: String, cat: List[String]): Unit = {
  writer.write(Document(Set(
    Field("id", id.toString),
    Field("title", title), Field("title_ja", title),
    Field("body", body), 
    Field("cat", cat), Field("cat_ja", cat)
  )))
}

val reader = new RecordReader("/tmp/jawiki.json",
    new JsonRecordParser[Article](classOf[Article])).filter(TypeFilter.STD_FILTER)

val ite = reader.iterator

val pattern: Regex = """Category:(.+)""".r
var id: Int = 0

ite.filterNot(_.getTitle().indexOf("曖昧さ回避") >= 0).foreach{a =>
  id += 1
  val title = a.getTitle()
  val body = a.getText()
  val cat = a.getCategories().map(
    _.getId() match {
      case pattern(a) => a
      case _ => null
    }
  ).filterNot(_ == null).toList

  addDocument(id, a.getTitle(), body, cat)

}

writer.close()

// print end time
new java.util.Date()
