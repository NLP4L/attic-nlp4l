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

import java.io.{PrintWriter, File}

import org.apache.lucene.search.spell.LuceneLevenshteinDistance
import org.nlp4l.core.{SchemaLoader, IReader}
import org.nlp4l.lm.{HmmTokenizer, HmmModel, HmmModelIndexer}

import scala.io.Source
import scala.util.matching.Regex
import scalax.file.Path

//------------------------------------
// create transliteration model
//------------------------------------

val indexModel = "/tmp/index-transliteration"

// remove index directory before creating it
val p = Path(new File(indexModel))
p.deleteRecursively()
val indexer = new HmmModelIndexer(indexModel)

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
val model = HmmModel(indexModel)

val tokenizer = HmmTokenizer(model)

def predict(katakana: String): String = {
  tokenizer.tokens(katakana).map(_.cls).mkString
}

//------------------------------------
// extract loan words
//------------------------------------

val index = "/tmp/index-jawiki"
val out = new PrintWriter("loanwords.txt")

val schema = SchemaLoader.loadFile("examples/schema/jawiki.conf")
val reader = IReader(index, schema)

val pattern: Regex = """([a-z]+) ([\u30A0-\u30FF]+)""".r
val lld = new LuceneLevenshteinDistance()

val THRESHOLD = 0.75

reader.field("ka_pair").get.terms.foreach{ t =>
  if(t.docFreq > 1){
    t.text match {
      case pattern(a, b) => {
        val predWord = predict(b)
        if(lld.getDistance(a, predWord) > THRESHOLD)
          out.printf("%s, %s\n", a, b)
      }
      case _ => {}
    }
  }
}

out.close
reader.close

// print end time
new java.util.Date()
