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

package org.nlp4l.syn

import java.io.{PrintWriter, File}

import org.apache.lucene.search.spell.LuceneLevenshteinDistance
import org.nlp4l.core.RawReader
import org.nlp4l.lm.{HmmTokenizer, HmmModel, HmmModelIndexer}

import scala.io.Source
import scala.util.matching.Regex
import scalax.file.Path

object LoanWordsExtractor {

  val DEF_MODELINDEX = "_transliteration"
  val DEF_THRESHOLD = "0.8"
  val DEF_MIN_DOCFREQ = "3"

  val usage =
  """
    |Usage:
    |LoanWordsExtractor
    |        --index <index dir>
    |        --field <field name>          field that is to be extracted
    |        [--threshold <threshold>]     threshold for Levenshtein distance (default is %s)
    |        [--docfreq <docFreq>]         minimum doc freq (default is %s)
    |        [--modelIndex <model index>]  name of the index for HMM training (default is %s)
    |        file                          output file name
  """.format(DEF_THRESHOLD, DEF_MIN_DOCFREQ, DEF_MODELINDEX).stripMargin

  def required(opts: Map[Symbol, String], key: Symbol): String = {
    val value = opts.getOrElse(key, null)
    if(value == null){
      println("%s must be set.".format(key.name))
      println(usage)
      sys.exit()
    }
    value
  }

  def main(args: Array[String]): Unit = {

    if (args.isEmpty){
      println(usage)
      sys.exit()
    }

    def parseOption(opts: Map[Symbol, String], files: List[String], list: List[String]): (Map[Symbol, String], List[String]) = list match {
      case Nil => (opts, files.reverse)
      case "--index" :: value :: tail => parseOption(opts + ('index -> value), files, tail)
      case "--field" :: value :: tail => parseOption(opts + ('field -> value), files, tail)
      case "--threshold" :: value :: tail => parseOption(opts + ('threshold -> value), files, tail)
      case "--docfreq" :: value :: tail => parseOption(opts + ('docfreq -> value), files, tail)
      case "--modelIndex" :: value :: tail => parseOption(opts + ('modelIndex -> value), files, tail)
      case value :: tail => parseOption(opts, value :: files, tail)
    }

    val (opts, files) = parseOption(Map(), List(), args.toList)

    val index = required(opts, 'index)
    val field = required(opts, 'field)
    val threshold = opts.getOrElse('threshold, DEF_THRESHOLD).toFloat
    val docFreq = opts.getOrElse('docfreq, DEF_MIN_DOCFREQ).toInt
    val modelIndex = opts.getOrElse('modelIndex, DEF_MODELINDEX)

    if (files.isEmpty){
      println("file must be specified.")
      println(usage)
      sys.exit()
    }
    else if (files.size > 1){
      println("Too many files are specified.")
      println(usage)
      sys.exit()
    }

    val trModel = TransliterationModel(modelIndex)

    val out = new PrintWriter(files(0))

    val reader = RawReader(index)

    val pattern: Regex = """([a-z]+) ([\u30A0-\u30FF]+)""".r
    val lld = new LuceneLevenshteinDistance()

    reader.field(field).get.terms.foreach{ t =>
      if(t.docFreq >= docFreq){
        t.text match {
          case pattern(a, b) => {
            val predWord = trModel.predict(b)
            if(lld.getDistance(a, predWord) > threshold)
              out.printf("%s, %s\n", a, b)
          }
          case _ => {}
        }
      }
    }

    out.close
    reader.close
  }
}

class TransliterationModel(index: String) {

  // remove index directory before creating it
  val p = Path(new File(index))
  p.deleteRecursively()
  val indexer = new HmmModelIndexer(index)

  val file = Source.fromFile("train_data/alpha_katakana_aligned.txt", "UTF-8")

  val pat1: Regex = """([\u30A0-\u30FF]+)([a-zA-Z]+)(.*)""".r

  def align(result: List[(String, String)], str: String): List[(String, String)] = {
    str match {
      case pat1(a, b, c) => {
        align(result :+ (a, b), c)
      }
      case _ => {
        result
      }
    }
  }

  // create hmm model index
  file.getLines.foreach{ line: String =>
    val doc = align(List.empty[(String, String)], line)
    indexer.addDocument(doc)
  }

  file.close

  indexer.close

  // read the model index
  private val model = HmmModel(index)

  private val tokenizer = HmmTokenizer(model)

  def predict(katakana: String): String = {
    tokenizer.tokens(katakana).map(_.cls).mkString
  }
}

object TransliterationModel {
  def apply(index: String) = new TransliterationModel(index)
}
