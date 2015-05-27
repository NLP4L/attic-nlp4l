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

import java.io.{PrintWriter, FileWriter, BufferedWriter, File}
import org.nlp4l.util.Adapter
import resource._

import org.nlp4l.core.{SchemaLoader, IReader}
import org.nlp4l.stats.TFIDF

import scala.annotation.tailrec


/**
 * Main for dump Vectors
 * Dumps feature vectors as space separated format
 */
object VectorsAdapter extends Adapter {
  def main(args: Array[String]): Unit = {
    val usage =
      """
        |Usage:
        |LabelPointAdapter
        |       -s <schema file>
        |       -f <feature field>
        |       [-t int|float]
        |       [--tfmode <TF mode>]
        |       [--smthterm <smoothing term>]
        |       [--idfmode <IDF mode>]
        |       [-d <data output file>]
        |       [-w <words output file>]
        |       [--features <feature1>{,<feature2>}]
        |       [--values <field1>{,<field2>}] [--valuesDir <dir>] [--valuesSep <sep>]
        |       <index dir>
      """.stripMargin
    val options = parseCommonOption(Map(), args.toList)
    if (!List('index, 'schema, 'field).forall(options.contains)) {
      println(usage)
      System.exit(1)
    }
    if (options.contains('type) && options('type).toLowerCase != "int" && options('type).toLowerCase != "float") {
      println(usage)
      System.exit(1)
    }

    val idxDir = options('index)
    val schemaFile = options('schema)
    val field = options('field)
    val vtype = options.getOrElse('type, "float").toLowerCase
    val tfMode = options.getOrElse('tfmode, "n")
    val smthterm = options.getOrElse('smthterm, "0.4").toDouble
    val idfMode = options.getOrElse('idfmode, "t")
    val out = options.getOrElse('data, "data.txt")
    val wordsOut = options.getOrElse('words, "words.txt")
    val words = if (options.contains('features)) options('features).split(",").toSet else Set.empty[String]
    val fNames = if (options.contains('values)) options('values).split(",").toList else List.empty[String]
    val valuesOutDir = options.getOrElse('valuesDir, "values")
    val valuesSep = options.getOrElse('valuesSep, ",")

    println("Index directory: " + idxDir)
    println("Schema file: " + schemaFile)
    println("Feature Field: " + field)
    println("Value type for vectors: " + vtype)
    println("TF mode: " + tfMode)
    println("Smooth term (for TF mode = \"m\"): " + smthterm)
    println("IDF mode: " + idfMode)
    println("Output vectors to: " + out)
    println("Output words to: " + wordsOut)
    println("(Optional) Features: " + words.mkString(","))
    println("(Optional) Additional values: " + fNames.mkString(","))
    println("(Optional) Additional values output to: " + valuesOutDir)
    println("(Optional) Additional values separator: " + valuesSep)


    val schema = SchemaLoader.loadFile(schemaFile)
    val reader = IReader(idxDir, schema)
    val docIds = reader.universalset().toList
    val (features, vectors) =
      if (vtype == "int")
        TFIDF.tfVectors(reader, field, docIds, words)
      else
        TFIDF.tfIdfVectors(reader, field, docIds, words, tfMode, smthterm, idfMode)
    dumpVectors(vectors, out)


    // output words
    val wordsFile = new File(wordsOut)
    for (output <- managed(new PrintWriter(new FileWriter(wordsFile)))) {
      features.foreach(output.println)
    }

    // output additional values
    // a file is created for each field
    if (fNames.nonEmpty) {
      val dir = new File(valuesOutDir)
      if (!dir.exists()) dir.mkdirs()
      val values = fieldValues(reader, docIds, fNames)
      fNames.foreach(fName => {
        val file = new File(valuesOutDir, "values_" + fName + ".txt")
        for (output <- managed(new PrintWriter(new FileWriter(file)))) {
          values.foreach(m => {
            val line = m.getOrElse(fName, List.empty).mkString(valuesSep)
            output.println(line)
          })
        }
      })
    }
  }

  def dumpVectors(vectors: => Stream[Seq[Any]], out: String = "data.txt"): Unit = {
    val file: File = new File(out)
    for(output <- managed(new BufferedWriter(new FileWriter(file)))) {
      @tailrec def loop(xs: Stream[Seq[Any]]): Unit = xs match {
        case Stream.Empty => ()
        case vec #:: tail => {
          for (v <- vec) output.write(v.toString + " ")
          output.newLine()
          loop(tail)
        }
      }
      loop(vectors)
      output.flush()
    }
  }

}
