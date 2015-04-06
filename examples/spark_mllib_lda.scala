import java.io.{FileWriter, BufferedWriter}

import resource._

import org.nlp4l.core.{IReader, SchemaLoader}
import org.nlp4l.spark.mllib.VectorsAdapter
import org.nlp4l.stats.{WordCounts, TFIDF}

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

def dumpWords(words: Vector[String], out: String): Unit = {
  for (output <- managed(new BufferedWriter(new FileWriter(out)))) {
    words.foreach(f => {output.write(f); output.newLine()})
  }
}

val indexDir = "/tmp/index-brown"
val schema = SchemaLoader.loadFile("examples/schema/brown.conf")
val reader = IReader(indexDir, schema)
val docs = reader.universalset()

// ex1)
// generate TF vectors
val (features, vectors) = TFIDF.tfVectors(reader, "body", docs.toList)
// output TF vectors
new VectorsAdapter().dumpVectors(vectors, "data.txt")
// output words
dumpWords(features, "words.txt")

// ex2)
val (features2, vectors2) = TFIDF.tfVectors(reader, "body_pos_nn", docs.toList)
new VectorsAdapter().dumpVectors(vectors2, "data_noun.txt")
dumpWords(features2, "words_noun.txt")

// ex3)
// select words with low df (high idf) values
val counts_df = WordCounts.countDF(reader, "body_pos_nn", Set.empty)
val highIDFwords = counts_df.filter(e => e._2 > 2 && e._2 < reader.numDocs / 2.0).map(_._1)
// generate TF vectors with selected words
val (features3, vectors3) = TFIDF.tfVectors(reader, "body_pos_nn", docs.toList, highIDFwords.toSet)
new VectorsAdapter().dumpVectors(vectors3, "data_noun_high_idf.txt")
dumpWords(features3, "words_high_idf.txt")
