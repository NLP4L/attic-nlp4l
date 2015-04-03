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

package org.nlp4l.spark.mllib

import java.io.{FileWriter, BufferedWriter, File}

import org.nlp4l.core.{IReader, SchemaLoader}
import org.nlp4l.stats.TFIDF
import resource._

/**
 * Experimental
 */
class LabeledPointAdapter {

  def dumpLabeledPoints(labels: Vector[Int], vectors: List[Vector[AnyVal]],  out: String = "data.txt"): Unit = {
    assert(labels.size == vectors.size)
    val file: File = new File(out)
    for(output <- managed(new BufferedWriter(new FileWriter(file)))) {
      labels.zip(vectors).foreach{case(label: Int, vector: Vector[AnyVal]) => {
        // output label
        output.write(label.toString)
        output.write(" ")
        // output index:value pairs for LIBSVM format. indices are one-based and in ascending order.
        val vecWithIdx = vector.zipWithIndex.filter(_._1 != 0).map(t => (t._2 + 1).toString + ":" + t._1.toString)
        output.write(vecWithIdx.mkString(" "))
        output.newLine()
      }}
      output.flush()
    }
  }
}

object LabeledPointAdapter {
  def main(args: Array[String]): Unit = {
    val idxDir = args(0)
    val schemaFile = args(1)
    val field = args(2)
    val out = if (args.size > 3) args(3) else "data.txt"
    val words_out = if (args.size > 4) args(4) else "words.txt"

    println("Index directory: " + idxDir)
    println("Schema file: " + schemaFile)
    println("Field: " + field)
    println("Output vectors to: " + out)
    println("Output words to: " + words_out)

    val schema = SchemaLoader.loadFile(schemaFile)
    val reader = IReader(idxDir, schema)
    val docs = reader.universalset()
    val words = Set("time", "book", "drink", "beer", "job", "walk")
    val (features, vectors) = TFIDF.tfIdfVectors(reader, field, docs.toList, words)
    val labels = Vector.fill(vectors.size)(1)
    new LabeledPointAdapter().dumpLabeledPoints(labels, vectors, out)

    // output words
    val writer = new BufferedWriter(new FileWriter(words_out))
    try {
      features.foreach(f => {writer.write(f); writer.newLine()})
    } finally {
      writer.close()
    }
  }
}
