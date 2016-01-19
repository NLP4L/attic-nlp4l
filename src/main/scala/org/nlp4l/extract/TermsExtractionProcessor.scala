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

package org.nlp4l.extract

import org.nlp4l.framework.models._
import org.nlp4l.framework.processors.{Processor, ProcessorFactory, DictionaryAttributeFactory}
import org.nlp4l.lucene.LuceneDocTermVector
import org.nlp4l.lucene.TermsExtractor
import org.nlp4l.lucene.TermsExtractor.Config
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

class TermsExtractionDictionaryAttributeFactory(settings: Map[String, String]) extends DictionaryAttributeFactory(settings) {
  override def getInstance: DictionaryAttribute = {
    val outScore = getBoolParam("outScore", true)
    val list = if(outScore){
      Seq[CellAttribute](
        CellAttribute("term", CellType.StringType, true, true),
        // use constant hashCode so that we don't take into account score when calculating hashCode of Records
        CellAttribute("score", CellType.FloatType, false, true, constantHashCode => 0)
      )
    }
    else {
      Seq[CellAttribute](
        CellAttribute("term", CellType.StringType, true, true)
      )
    }
    new DictionaryAttribute("terms", list)
  }
}

class TermsExtractionProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {

  override def getInstance: Processor = {
    val logger = LoggerFactory.getLogger(this.getClass)
    val config = new Config()
    config.index = getStrParamRequired("index")
    config.outScore = getBoolParam("outScore", true)
    config.fieldCn = getStrParamRequired("field")
    config.fieldLn2 = settings.getOrElse("fieldln2", null)
    config.fieldRn2 = settings.getOrElse("fieldrn2", null)
    config.delimiter = settings.getOrElse("delimiter", "/")
    config.outNum = getIntParam("num", org.nlp4l.lucene.TermsExtractor.DEF_OUT_NUM)
    config.scorer = settings.getOrElse("scorer", "FreqDFLR")
    logger.info(
      """TermsExtractionProcessor starts with parameters
        |    index "{}"
        |    field "{}"
        |    fieldln2 "{}"
        |    fieldrn2 "{}"
        |    delimiter "{}"
        |    num "{}"
        |    scorer "{}"
        |    outScore "{}"""".stripMargin,
      config.index, config.fieldCn, config.fieldLn2, config.fieldRn2, config.delimiter, config.outNum.toString, config.scorer, config.outScore.toString)
    new TermsExtractionProcessor(config)
  }
}

class TermsExtractionProcessor(val config: Config) extends Processor {

  override def execute(data: Option[Dictionary]): Option[Dictionary] = {
    val te = new ProcTermsExtractor(config)
    te.setConfig()
    te.execute()
    Some(Dictionary(te.records))
  }
}

class ProcTermsExtractor(config: Config) extends TermsExtractor(config: Config) {

  val records = ListBuffer.empty[Record]
  val logger = LoggerFactory.getLogger(this.getClass)

  override def printResultEntry(e: java.util.Map.Entry[String, LuceneDocTermVector.TermWeight]): Unit ={
    if(config.outScore){
      records += Record(Seq(Cell("term", getTerm(e)), Cell("score", getScore(e))))
    }
    else{
      records += Record(Seq(Cell("term", getTerm(e))))
    }
  }
}
