/*
 * Copyright 2015 org.NLP4L
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

package org.nlp4l.colloc

import org.nlp4l.core.RawReader
import org.nlp4l.framework.processors._
import org.nlp4l.framework.models._
import org.slf4j.LoggerFactory

class BuddyWordsDictionaryAttributeFactory(settings: Map[String, String]) extends DictionaryAttributeFactory(settings) {
  override def getInstance: DictionaryAttribute = {

    val list = Seq[CellAttribute](
      CellAttribute("word", CellType.StringType, false, true),
      CellAttribute("buddies", CellType.StringType, false, true)
    )
    new DictionaryAttribute("buddyWords", list)
  }
}

class BuddyWordsProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {

  val DEF_MAX_DOCS_TO_ANALYZE: Int = 1000
  val DEF_SLOP: Int = 5
  val DEF_MAX_COI_TERMS_PER_TERM: Int = 20
  val DEF_MAX_BASE_TERMS_PER_DOC: Int = 10 * 1000

  override def getInstance: Processor = {
    val index = getStrParamRequired("index")
    val field = getStrParamRequired("field")
    val srcField = field            // use same field name for source field for now
    val maxDocsToAnalyze = getIntParam("maxDocsToAnalyze", DEF_MAX_DOCS_TO_ANALYZE)
    val slop = getIntParam("slop", DEF_SLOP)
    val maxCoiTermsPerTerm = getIntParam("maxCoiTermsPerTerm", DEF_MAX_COI_TERMS_PER_TERM)
    val maxBaseTermsPerDoc = getIntParam("maxBaseTermsPerDoc", DEF_MAX_BASE_TERMS_PER_DOC)
    new BuddyWordsProcessor(index, field, srcField, maxDocsToAnalyze, slop, maxCoiTermsPerTerm, maxBaseTermsPerDoc)
  }
}

class BuddyWordsProcessor(val index: String, val field: String, val srcField: String, val maxDocsToAnalyze: Int,
                          val slop: Int, val maxCoiTermsPerTerm: Int, val maxBaseTermPerDoc: Int) extends Processor {

  override def execute(data: Option[Dictionary]): Option[Dictionary] = {
    val logger = LoggerFactory.getLogger(this.getClass)
    val reader = RawReader(index)
    var records = scala.collection.mutable.Seq.empty[Record]
    try{
      var progress = 0
      val terms = reader.field(srcField).get.terms
      val len = terms.length
      val finder = BuddyWordsFinder(reader, maxDocsToAnalyze, slop, maxCoiTermsPerTerm, maxBaseTermPerDoc)
      terms.foreach{ t =>
        val result = finder.find(field, t.text)
        progress = progress + 1
        if((progress % 1000) == 0){
          val percent = ((progress.toFloat / len) * 100).toInt
          logger.info(s"$percent % done ($progress / $len) term is ${t.text}")
        }
        if(result.size > 0){
          records = records :+ Record(Seq(Cell("word", t.text), Cell("buddies", result.map(_._1).mkString(","))))
        }
      }
      Some(Dictionary(records))
    }
    finally{
      if(reader != null) reader.close
    }
  }
}
