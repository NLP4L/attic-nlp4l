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

package org.nlp4l.extract

object TermsExtractor {

  val usage =
  """
    |Usage:
    |TermsExtractor
    |        --field <field name>
    |        [--fieldLn2 <field name for ln2>]            default is <field name>_ln2
    |        [--fieldRn2 <field name for rn2>]            default is <field name>_rn2
    |        [--delimiter <delimiter str>]                default is "/" (a slash)
    |        [--out <output file name>]                   default output is stdout
    |        [--noscore]                                  specify when you don't need score
    |        [--num <num of output>]                      specify number of terms (default is 1000)
    |        [--scorer <scorer name>]                     specify one of FreqDFLR(default), FreqLR, TypeCountDFLR or TypeCountLR
    |        index                                        specify index directory
  """.stripMargin

  def required(opts: Map[Symbol, String], key: Symbol): String = {
    val value = opts.getOrElse(key, null)
    if(value == null){
      println("%s must be set.\n\n".format(key.name))
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
        println("no index is specified\n\n")
        println(usage)
        sys.exit()
      }
      case "--field" :: value :: tail => parseOption(opts + ('field -> value), tail)
      case "--fieldLn2" :: value :: tail => parseOption(opts + ('fieldln2 -> value), tail)
      case "--fieldRn2" :: value :: tail => parseOption(opts + ('fieldrn2 -> value), tail)
      case "--delimiter" :: value :: tail => parseOption(opts + ('delimiter -> value), tail)
      case "--out" :: value :: tail => parseOption(opts + ('out -> value), tail)
      case "--noscore" :: tail => parseOption(opts + ('score -> false.toString), tail)
      case "--num" :: value :: tail => parseOption(opts + ('num -> value), tail)
      case "--scorer" :: value :: tail => parseOption(opts + ('scorer -> value), tail)
      case value :: Nil => (opts, value)
      case _ => {
        println("too many indexes are specified")
        sys.exit()
      }
    }

    val (opts, out) = parseOption(Map(), args.toList)

    val config = new org.nlp4l.lucene.TermsExtractor.Config()
    config.index = out
    config.fieldCn = required(opts, 'field)
    config.fieldLn2 = opts.getOrElse('fieldln2, null)
    config.fieldRn2 = opts.getOrElse('fieldrn2, null)
    config.delimiter = opts.getOrElse('delimiter, "/")
    config.outFile = opts.getOrElse('out, null)
    config.outScore = opts.getOrElse('score, true.toString).toBoolean
    config.outNum = optInt(opts, 'num, org.nlp4l.lucene.TermsExtractor.DEF_OUT_NUM)
    config.scorer = opts.getOrElse('scorer, "FreqDFLR")

    val te = org.nlp4l.lucene.TermsExtractor.getExtractor(config)
    te.execute();
  }
}
