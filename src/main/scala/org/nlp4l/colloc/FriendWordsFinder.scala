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

package org.nlp4l.colloc

import java.io.PrintWriter

import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.util.BytesRef
import org.nlp4l.core._
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.lucene.FriendWordsFinderTermFilter

class FriendWordsFinder(reader: RawReader, maxDocsToAnalyze: Int, slop: Int, maxCoiTermsPerTerm: Int, maxBaseTermsPerDoc: Int,
                        baseTermFilter: FriendWordsFinderTermFilter, coiTermFilter: FriendWordsFinderTermFilter) {

  val finder = new org.nlp4l.lucene.FriendWordsFinder(reader.ir, maxDocsToAnalyze, slop,
    maxCoiTermsPerTerm, maxBaseTermsPerDoc, baseTermFilter, coiTermFilter)

  def find(field: String, word: String): Array[(String, Float)] = {
    val result = finder.findCoincidentalTerms(field, new BytesRef(word))
    if(result == null || result.length == 0) Array[(String, Float)]()
    else{
      result.map(s => (s.coiTerm, s.score))
    }
  }
}

object FriendWordsFinder {

  val DEF_MAX_DOCS_TO_ANALYZE: Int = 1000
  val DEF_SLOP: Int = 5
  val DEF_MAX_COI_TERMS_PER_TERM: Int = 20
  val DEF_MAX_BASE_TERMS_PER_DOC: Int = 10 * 1000

  def apply(reader: RawReader,
            maxDocsToAnalyze: Int = DEF_MAX_DOCS_TO_ANALYZE,
            slop: Int = DEF_SLOP,
            maxCoiTermsPerTerm: Int = DEF_MAX_COI_TERMS_PER_TERM,
            maxBaseTermsPerDoc: Int = DEF_MAX_BASE_TERMS_PER_DOC,
            termFilter: FriendWordsFinderTermFilter = FriendWordsNullStopFilter()) =
    new FriendWordsFinder(reader, maxDocsToAnalyze, slop, maxCoiTermsPerTerm, maxBaseTermsPerDoc, termFilter, termFilter)

  val usage =
  """
    |Usage:
    |FriendWordsFinder
    |       --index <index dir>
    |       --field <field name>
    |       [--srcField <source field name>]
    |       [--text]                          specify this option when you want a text for output instead of index
    |       [--maxDocsToAnalyze <num>]        default is 1000
    |       [--slop <num>]                    default is 5
    |       [--maxCoiTermsPerTerm <num>]      default is 20
    |       [--maxBaseTermsPerDoc <num>]      default is 10000
    |       out_index                         specify output index directory (or name of text file when you use --text)
  """.stripMargin

  def required(opts: Map[Symbol, String], key: Symbol): String = {
    val value = opts.getOrElse(key, null)
    if(value == null){
      println("%s must be set.".format(key.name))
      println(usage)
      sys.exit()
    }
    value
  }

  def optInt(opts: Map[Symbol, String], opt: Symbol, defval: Int): Int = {
    val value = opts.getOrElse(opt, defval.toString)
    value.toInt
  }

  def main(args: Array[String]): Unit = {

    if (args.isEmpty){
      println(usage)
      sys.exit()
    }

    def parseOption(opts: Map[Symbol, String], list: List[String]): (Map[Symbol, String], String) = list match {
      case Nil => {
        println("no index or file is specified")
        sys.exit()
      }
      case "--index" :: value :: tail => parseOption(opts + ('index -> value), tail)
      case "--field" :: value :: tail => parseOption(opts + ('field -> value), tail)
      case "--srcField" :: value :: tail => parseOption(opts + ('srcField -> value), tail)
      case "--text" :: tail => parseOption(opts + ('text -> true.toString), tail)
      case "--maxDocsToAnalyze" :: value :: tail => parseOption(opts + ('maxDocsToAnalyze -> value), tail)
      case "--slop" :: value :: tail => parseOption(opts + ('slop -> value), tail)
      case "--maxCoiTermsPerTerm" :: value :: tail => parseOption(opts + ('maxCoiTermsPerTerm -> value), tail)
      case "--maxBaseTermPerDoc" :: value :: tail => parseOption(opts + ('maxBaseTermPerDoc -> value), tail)
      case value :: Nil => (opts, value)
      case _ => {
        println("too many indexes or files are specified")
        sys.exit()
      }
    }

    val (opts, out) = parseOption(Map(), args.toList)

    val index = required(opts, 'index)
    val field = required(opts, 'field)
    val srcField = opts.getOrElse('srcField, field)
    val text = opts.getOrElse('text, false.toString).toBoolean
    val maxDocsToAnalyze = optInt(opts, 'maxDocsToAnalyze, DEF_MAX_DOCS_TO_ANALYZE)
    val slop = optInt(opts, 'slop, DEF_SLOP)
    val maxCoiTermsPerTerm = optInt(opts, 'maxCoiTermsPerTerm, DEF_MAX_COI_TERMS_PER_TERM)
    val maxBaseTermPerDoc = optInt(opts, 'maxBaseTermPerDoc, DEF_MAX_BASE_TERMS_PER_DOC)

    val reader = RawReader(index)
    val writer =
      if(text) new PrintWriter(out, "UTF-8")
      else new IWriter(out, schema())
    try{
      val terms = reader.field(srcField).get.terms
      val finder = FriendWordsFinder(reader, maxDocsToAnalyze, slop, maxCoiTermsPerTerm, maxBaseTermPerDoc)
      terms.foreach{ t =>
        val result = finder.find(field, t.text)
        if(result.size > 0){
          if(text){
            writer.asInstanceOf[PrintWriter].printf("%s => %s\n", t.text, result.map(_._1).mkString(","))
          }
          else{
            val source = Field("source", List(t.text))
            val friends = Field("friends", result.map(_._1))
            writer.asInstanceOf[IWriter].write(Document(Set(source, friends)))
          }
        }
      }
    }
    finally{
      if(writer != null){
        if(writer.isInstanceOf[PrintWriter]) writer.asInstanceOf[PrintWriter].close()
        else writer.asInstanceOf[IWriter].close()
      }
      if(reader != null) reader.close
    }
  }

  def schema(): Schema = {
    val source = FieldType(null, true, true)
    val friends = FieldType(null, true, true)
    Schema(new Analyzer(new KeywordAnalyzer()), Map("source" -> source, "friends" -> friends))
  }
}
