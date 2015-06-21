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

package org.nlp4l.nee

import opennlp.tools.sentdetect._
import opennlp.tools.tokenize._
import opennlp.tools.namefind._
import opennlp.tools.util.Span

import scala.collection.mutable.ArrayBuffer
import scalax.io.Resource

class OpenNLPExtractor(sdModelFile: String, tokModelFile: String, nameModelFile: String) {

  val isSd: java.io.InputStream = Resource.fromFile(sdModelFile).inputStream.open().get
  val isTok: java.io.InputStream = Resource.fromFile(tokModelFile).inputStream.open().get
  val isName: java.io.InputStream = Resource.fromFile(nameModelFile).inputStream.open().get

  val sdModel = new SentenceModel(isSd)
  val sentDetector = new SentenceDetectorME(sdModel)

  val tokModel = new TokenizerModel(isTok)
  val tokenizer = new TokenizerME(tokModel)

  val extModel	= new TokenNameFinderModel(isName)
  val extractor = new NameFinderME(extModel)

  def extractNamedEntities(doc: String): Seq[(String, String)] = {
    val result: scala.collection.mutable.ArrayBuffer[(String, String)] = new ArrayBuffer[(String, String)]()
    val sentences = sentDetector.sentDetect(doc)
    sentences.foreach{ sentence =>
      val tokens = tokenizer.tokenize(sentence)
      val names = extractor.find(tokens)
      if(names != null && names.size > 0){
        val entities = Span.spansToStrings(names, tokens)
        result ++= (entities zip names).map(a => (a._1, a._2.getType()))
      }
    }
    extractor.clearAdaptiveData()

    result.toSeq
  }

  def close(): Unit = {
    if(isName != null) isName.close()
    if(isTok != null) isTok.close()
    if(isSd != null) isSd.close()
  }
}

object OpenNLPExtractor {
  def apply(sdModelFile: String, tokModelFile: String, nameModelFile: String) =
    new OpenNLPExtractor(sdModelFile, tokModelFile, nameModelFile)
}
