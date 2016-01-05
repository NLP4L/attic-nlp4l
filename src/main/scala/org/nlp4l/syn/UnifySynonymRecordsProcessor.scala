/*
 * Copyright 2016 org.NLP4L
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

package org.nlp4l.syn

import org.nlp4l.framework.models._
import org.nlp4l.framework.processors.{Processor, ProcessorFactory, DictionaryAttributeFactory}
import org.slf4j.LoggerFactory

class UnifySynonymRecordsDictionaryAttributeFactory(settings: Map[String, String]) extends DictionaryAttributeFactory(settings) {
  override def getInstance: DictionaryAttribute = {

    val list = Seq[CellAttribute](
      CellAttribute("synonyms", CellType.StringType, false, true)
    )
    new DictionaryAttribute("unifySynonymRecords", list)
  }
}

class UnifySynonymRecordsProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {

  override def getInstance: Processor = {
    val logger = LoggerFactory.getLogger(this.getClass)
    val separator = settings.getOrElse("separator", ",")
    val sortReverse = getBoolParam("sortReverse", false)
    logger.info("""separator "{}", sortReverse "{}"""", separator, sortReverse)
    new UnifySynonymRecordsProcessor(sortReverse, separator)
  }
}

class UnifySynonymRecordsProcessor(val sortReverse: Boolean, val separator: String) extends Processor {

  override def execute(data: Option[Dictionary]): Option[Dictionary] = {
    data match {
      case None => None
      case Some(dic) => {
        val inputRecords = dic.recordList.map{ r =>
          r.cellList.map{ c => c.value.toString }
        }
        val uniqueRecords = SynonymCommon.getUniqueRecords(inputRecords, Seq())
        Some(Dictionary(for(r <- uniqueRecords) yield {
          Record(Seq(Cell("synonyms", r.mkString(separator))))
        }))
      }
    }
  }
}
