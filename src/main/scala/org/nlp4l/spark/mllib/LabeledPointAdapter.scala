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

import java.io._

import org.nlp4l.core.{RawReader, IReader, SchemaLoader}
import org.nlp4l.stats.TFIDF
import resource._

/**
 * Experimental
 */
class LabeledPointAdapter {

  def dumpLabeledPoints(labels: Vector[Int], vectors: List[Vector[AnyVal]], out: String = "data.txt"): Unit = {
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

/**
 * Main for dump LabeledPointAdapter
 * Dumps feature vectors as LIBSVM format.
 */
object LabeledPointAdapter {
  def main(args: Array[String]): Unit = {
    val usage =
      """
        |Usage:
        |LabelPointAdapter
        |       -s <schema file>
        |       -f <feature field>
        |       -l <label field>
        |       [--labelfile <label mapping file>]
        |       [--labelfileSep <sep>]
        |       [-d <data output file>]
        |       [-w <words output file>]
        |       [--features <feature1>{,<feature2>}]
        |       [--values <field1>{,<field2>}] [--valuesDir <dir>] [--valuesSep <sep>]
        |       <index dir>
      """.stripMargin
    def parseOption(parsed: Map[Symbol, String], list: List[String]): Map[Symbol, String] = list match {
      case Nil => parsed
      case "-s" :: value :: tail => parseOption(parsed + ('schema -> value), tail)
      case "-f" :: value :: tail => parseOption(parsed + ('field -> value), tail)
      case "-l" :: value :: tail => parseOption(parsed + ('label -> value), tail)
      case "--labelfile" :: value :: tail => parseOption(parsed + ('labelfile -> value), tail)
      case "--labelfileSep" :: value :: tail => parseOption(parsed + ('labelfileSep -> value), tail)
      case "-d" :: value :: tail => parseOption(parsed + ('data -> value), tail)
      case "-w" :: value :: tail => parseOption(parsed + ('words -> value), list)
      case "--features" :: value :: tail => parseOption(parsed + ('features -> value), tail)
      case "--values" :: value :: tail => parseOption(parsed + ('values -> value), tail)
      case "--valuesDir" :: value :: tail => parseOption(parsed + ('valuesDir -> value), tail)
      case "--valuesSep" :: value :: tail => parseOption(parsed + ('valuesSep -> value), tail)
      case value :: tail => parseOption(parsed + ('index -> value), tail)
    }
    val options = parseOption(Map(), args.toList)
    if (!List('index, 'schema, 'label, 'field).forall(options.contains)) {
      println(usage)
      System.exit(1)
    }

    val idxDir = options('index)
    val schemaFile = options('schema)
    val field = options('field)
    val labelField = options('label)
    val labelFile = options.getOrElse('labelfile, "label.txt")
    val labelFileSep = options.getOrElse('labelfileSep, "\t")
    val out = options.getOrElse('data, "data.txt")
    val wordsOut = options.getOrElse('words, "words.txt")
    val words = if (options.contains('features)) options('features).split(",").toSet else Set.empty[String]
    val fNames = options.getOrElse('values, "").split(",").toList
    val valuesOutDir = options.getOrElse('valuesDir, ".")
    val valuesSep = options.getOrElse('valuesSep, ",")

    println("Index directory: " + idxDir)
    println("Schema file: " + schemaFile)
    println("Feature Field: " + field)
    println("Label Field: " + labelField)
    println("Label Mapping File" + labelFile)
    println("Output vectors to: " + out)
    println("Output words to: " + wordsOut)
    println("(Optional) Features: " + words.mkString(","))
    println("(Optional) Additional values: " + fNames.mkString(","))
    println("(Optional) Additional values output to: " + valuesOutDir)
    println("(Optional) Additional values separator: " + valuesSep)

    val schema = SchemaLoader.loadFile(schemaFile)
    val reader = IReader(idxDir, schema)

    def makeLabelMap: Map[String, Int] = {
      val file = new File(labelFile)
      if (file.exists()) {
        // generate mappings from existing file
        val builder = Map.newBuilder[String, Int]
        for (input <- managed(new BufferedReader(new FileReader(file)))) {
          def read(): Unit = input.readLine() match {
            case null => ()
            case line => {
              val cols = line.split(labelFileSep)
              builder += (cols(0) -> cols(1).toInt)
              read()
            }
          }
          read()
        }
        builder.result()
      } else {
        // generate mappings from index
        // labels (integer value) are automatically generated.
        val labelMap = reader.field(labelField) match {
          case Some(fieldInfo) => fieldInfo.terms.map(_.text).zipWithIndex.toMap
          case _ => Map.empty[String, Int]
        }
        // output generated mappings to file for next use.
        for (output <- managed(new PrintWriter(file))) {
          labelMap.toList sortBy(x => x._2) foreach { case (value, label) =>
              output.println("%s%s%d".format(value, labelFileSep, label))
          }
        }
        labelMap
      }
    }

    val docIds = reader.universalset().toList

    val labelMap = makeLabelMap
    val labels = fieldValues(reader, docIds, Seq(labelField)).map(m => m(labelField)(0)).map(labelMap(_)).toVector

    //val words = Set("time", "book", "drink", "beer", "job", "walk")
    val (features, vectors) = TFIDF.tfIdfVectors(reader, field, docIds, words)
    //val labels = Vector.fill(vectors.size)(1)
    new LabeledPointAdapter().dumpLabeledPoints(labels, vectors, out)

    // output words
    val wordsFile = new File(wordsOut)
    for (output <- managed(new PrintWriter(new FileWriter(wordsFile)))) {
      features.foreach(output.println)
    }

    // output additional values
    // a file is created for each field
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

  private def fieldValues(reader: RawReader, docIds: List[Int], fields: Seq[String]): List[Map[String, List[String]]] = {
    docIds.map(id => reader.document(id) match {
      case Some(doc) => {
        fields.map(f => (f, doc.getValue(f).getOrElse(List.empty))).toMap
      }
      case _ => Map.empty[String, List[String]]
    })
  }

}
