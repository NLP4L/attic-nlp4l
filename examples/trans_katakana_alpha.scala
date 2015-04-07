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

import org.nlp4l.lm._

import scala.io._
import scala.util.matching.Regex
import scalax.file.Path

val index = "/tmp/index-transliteration"

// remove index directory before creating it
val p = Path(new File(index))
p.deleteRecursively()
val indexer = new HmmModelIndexer(index)

val file = Source.fromFile("train_data/alpha_katakana_aligned.txt", "UTF-8")

val pattern: Regex = """([\u30A0-\u30FF]+)([a-zA-Z]+)(.*)""".r

def align(result: List[(String, String)], str: String): List[(String, String)] = {
  str match {
    case pattern(a, b, c) => {
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
val model = HmmModel(index)

// print the model
println("\n=== classes ===")
val tableSize = model.classNum
for(i <- 0 to tableSize - 1){
  println(model.className(i))
}

println("\n=== classNamesDic ===")
model.classNamesDic.foreach{
  println(_)
}

println("\n=== costInitialState ===")
model.costInitialState.foreach{
  println(_)
}

println("\n=== costConnection ===")
print("   ")
for(i <- 0 to tableSize - 1){
  print("%10s".format(model.className(i)))
}
println
for(i <- 0 to tableSize - 1){
  print("%4s ".format(model.className(i)))
  for(j <- 0 to tableSize - 1){
    print("%8d  ".format(model.costConnection(i)(j)))
  }
  println
}

println("\n=== words ===")
model.words.foreach{
  println(_)
}

println("\n=== wordDic ===")
model.wordDic.foreach{
  println(_)
}

println("\n=== wordClasses ===")
model.wordClasses.foreach{
  println(_)
}

println("\n=== wordDic(tempDic) ===")
model.tempDic.foreach{ e =>
  println(e._1)
  e._2.foreach{ g =>
    println("\t%s %d (%d) %d".format(model.className(g._1), model.classFreq(g._1), g._1, g._2))
  }
}

println("\n=== tokenizer test ===")
val tokenizer = HmmTokenizer(model)

tokenizer.tokens("フランス")
tokenizer.tokens("エンタープライズ")
tokenizer.tokens("パナソニック")
tokenizer.tokens("アクション")
tokenizer.tokens("プログラム")
tokenizer.tokens("ポイント")
