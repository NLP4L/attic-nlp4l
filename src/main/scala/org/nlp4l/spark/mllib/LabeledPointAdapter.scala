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
import org.nlp4l.mahout.RandomForestVectorsAdapter._
import org.nlp4l.stats.TFIDF
import org.nlp4l.util.{FeatureSelector, Adapter}
import resource._

/**
 * Main for dump LabeledPoints
 * Dumps feature vectors as LIBSVM format.
 */
object LabeledPointAdapter extends Adapter with FeatureSelector {
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
        |       -l <label field>
        |       [--labelfileSep <sep>]
        |       [-o <outdir>]
        |       [--features <feature1>{,<feature2>}]
        |       [--values <field1>{,<field2>}] [--valuesSep <sep>]
        |       [--maxDFPercent maxDFPercent]
        |       [--minDF minDF]
        |       [--maxFeatures maxFeatures]
        |       <index dir>
      """.stripMargin
    def parseOption(parsed: Map[Symbol, String], list: List[String]): Map[Symbol, String] = list match {
      case Nil => parsed
      case "-l" :: value :: tail => parseOption(parsed + ('label -> value), tail) ++ parseCriteriaOption(Map(), args.toList)
      case "--labelfileSep" :: value :: tail => parseOption(parsed + ('labelfileSep -> value), tail)
      case value :: tail => parseOption(parsed, tail)
    }
    val options = parseCommonOption(Map(), args.toList) ++ parseOption(Map(), args.toList)
    if (!List('index, 'schema, 'label, 'field).forall(options.contains)) {
      println(usage)
      System.exit(1)
    }
    if (options.contains('type) && options('type).toLowerCase != "int" && options('type).toLowerCase != "float") {
      println(usage)
      System.exit(1)
    }

    val idxDir = options('index)
    val schemaFile = options('schema)
    val outdir = options.getOrElse('outdir, "labeled-point-out")
    val field = options('field)
    val vtype = options.getOrElse('type, "float").toLowerCase
    val tfMode = options.getOrElse('tfmode, "n")
    val smthterm = options.getOrElse('smthterm, "0.4").toDouble
    val idfMode = options.getOrElse('idfmode, "t")
    val labelField = options('label)
    val labelFile = outdir + File.separator + "label.txt"
    val labelFileSep = options.getOrElse('labelfileSep, "\t")
    val out = outdir + File.separator + "data.txt"
    val wordsOut = outdir + File.separator + "words.txt"
    val words = if (options.contains('features)) options('features).split(",").toSet else Set.empty[String]
    val fNames = if (options.contains('values)) options('values).split(",").toList else List.empty[String]
    val valuesOutDir = outdir + File.separator + "values"
    val valuesSep = options.getOrElse('valuesSep, ",")
    val maxDFPercent = if (options.contains('maxDFPercent)) options('maxDFPercent).toDouble / 100.0 else 0.99
    val minDF = if (options.contains('minDF)) options('minDF).toInt else 1
    val maxFeatures = if (options.contains('maxFeatures)) options('maxFeatures).toInt else -1


    println("Index directory: " + idxDir)
    println("Schema file: " + schemaFile)
    println("Feature Field: " + field)
    println("Value type for vectors: " + vtype)
    println("TF mode: " + tfMode)
    println("Smooth term (for TF mode = \"m\"): " + smthterm)
    println("IDF mode: " + idfMode)
    println("Label Field: " + labelField)
    println("Output directory: " + outdir)
    println("Max DF Percent: " + maxDFPercent)
    println("Min DF: " + minDF)
    println("Max Number of Features: " + maxFeatures)
    println("(Optional) Features: " + words.mkString(","))
    println("(Optional) Additional values: " + fNames.mkString(","))
    println("(Optional) Additional values separator: " + valuesSep)

    val schema = SchemaLoader.loadFile(schemaFile)
    val reader = IReader(idxDir, schema)

    val dir = new File(outdir)
    if (!dir.exists()) dir.mkdirs()

    // select features (if not specified)
    val words2 =
      if (words.isEmpty)
        selectFeatures(reader, field, minDF, maxDFPercent, maxFeatures)
      else words

    val docIds = reader.universalset().toList
    val labelMap = makeLabelMap(reader, labelField, labelFile, labelFileSep)
    val labels = fieldValues(reader, docIds, Seq(labelField)).map(m => m(labelField).head).map(labelMap(_)).toVector
    val (features, vectors) =
      if (vtype == "int")
        TFIDF.tfVectors(reader, field, docIds, words2)
      else
        TFIDF.tfIdfVectors(reader, field, docIds, words2, tfMode, smthterm, idfMode)
    dumpLabeledPoints(labels, vectors, out)

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

  def makeLabelMap(reader: RawReader, labelField: String, labelFile: String, labelFileSep: String): Map[String, Int] = {
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

  def dumpLabeledPoints(labels: Vector[Int], vectors: Stream[Seq[AnyVal]], out: String): Unit = {
    val file: File = new File(out)
    for(output <- managed(new BufferedWriter(new FileWriter(file)))) {
      labels.zip(vectors).foreach{case(label: Int, vector: Seq[AnyVal]) => {
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
