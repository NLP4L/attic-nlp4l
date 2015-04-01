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

import org.nlp4l.core.{SchemaLoader, IReader}
import org.nlp4l.stats.TFIDF

/**
 * Experimental.
 */
class LDAAdapter {
  def dumpVectors(features: Vector[String], vectors: List[Vector[Long]], out: String = "data.txt"): Unit = {
    val file: File = new File(out)
    val writer: BufferedWriter = new BufferedWriter(new FileWriter(file))
    try {
      vectors.foreach(vec => {
        writer.write(vec.mkString(" "))
        writer.newLine()
      })
    } finally {
      writer.close
    }
  }
}

object LDAAdapter {
  def main(args: Array[String]): Unit = {
    val idxDir = args(0)
    val schemaFile = args(1)
    val field = args(2)
    val out = if (args.size > 3) args(3) else "data.txt"

    println("Index directory: " + idxDir)
    println("Schema file: " + schemaFile)
    println("Field: " + field)
    println("Output to: " + out)

    val schema = SchemaLoader.loadFile(schemaFile)
    val reader = IReader(idxDir, schema)
    val docs = reader.universalset()
    val (features, vectors) = TFIDF.tfVectors(reader, field, docs.toList)
    new LDAAdapter().dumpVectors(features, vectors, out)
  }
}
