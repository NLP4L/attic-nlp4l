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
import org.nlp4l.util.{FeatureSelector, Adapter}
import resource._

import org.nlp4l.core.{SchemaLoader, IReader}
import org.nlp4l.stats.TFIDF

import scala.annotation.tailrec


/**
 * Main for dump Vectors
 * Dumps feature vectors as space separated format
 */
object VectorsAdapter extends Adapter with FeatureSelector {
  def main(args: Array[String]): Unit = {
    val usage =
      """
        |Usage:
        |VectorsAdapter
        |       -s <schema file>
        |       -f <feature field>
        |       [--id <id field>]
        |       [--header]
        |       [--type int|float]
        |       [--tfmode <TF mode>]
        |       [--smthterm <smoothing term>]
        |       [--idfmode <IDF mode>]
        |       [-o <outdir>]
        |       [--features <feature file>]
        |       [--outputSep <sep>]
        |       [--values <field1>{,<field2>}] [--valuesSep <sep>]
        |       [--maxDFPercent <maxDFPercent>]
        |       [--minDF <minDF>]
        |       [--maxFeatures <maxFeatures>]
        |       [--boosts <term boosts file>]
        |       <index dir>
      """.stripMargin

    def parseOption(parsed: Map[Symbol, String], list: List[String]): Map[Symbol, String] = list match {
      case Nil => parsed
      case "--id" :: value :: tail => parseOption(parsed + ('idfield -> value), tail)
      case "--header" :: tail => parseOption(parsed + ('header -> "true"), tail)
      case value :: tail => parseOption(parsed, tail)
    }

    val options = parseCommonOption(Map(), args.toList) ++ parseCriteriaOption(Map(), args.toList) ++ parseOption(Map(), args.toList)
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
    val outdir = options.getOrElse('outdir, "vectors-out")
    val field = options('field)
    val idField = options.getOrElse('idfield, "")
    val header = if (options.contains('header)) true else false
    val vtype = options.getOrElse('type, "float").toLowerCase
    val tfMode = options.getOrElse('tfmode, "n")
    val smthterm = options.getOrElse('smthterm, "0.4").toDouble
    val idfMode = options.getOrElse('idfmode, "t")
    val out = outdir + File.separator + "data.txt"
    val wordsOut = outdir + File.separator + "words.txt"
    val words = if (options.contains('features)) readFeatures(options('features)) else Set.empty[String]
    val fNames = if (options.contains('values)) options('values).split(",").toList else List.empty[String]
    val outputSep = options.getOrElse('outputSep, " ")
    val valuesOutDir = outdir + File.separator + "values"
    val valuesSep = options.getOrElse('valuesSep, ",")
    val maxDFPercent = if (options.contains('maxDFPercent)) options('maxDFPercent).toDouble / 100.0 else 0.99
    val minDF = if (options.contains('minDF)) options('minDF).toInt else 1
    val maxFeatures = if (options.contains('maxFeatures)) options('maxFeatures).toInt else -1
    val termBoosts = if (options.contains('boosts)) readTermBoosts(options('boosts)) else Map.empty[String, Double]

    println("Index directory: " + idxDir)
    println("Schema file: " + schemaFile)
    println("Feature Field: " + field)
    println("Id Field: " + idField)
    println("Header: " + header.toString)
    println("Value type for vectors: " + vtype)
    println("TF mode: " + tfMode)
    println("Smooth term (for TF mode = \"m\"): " + smthterm)
    println("IDF mode: " + idfMode)
    println("Output directory: " + outdir)
    println("Output values separator: " + outputSep)
    println("Max DF Percent: " + maxDFPercent)
    println("Min DF: " + minDF)
    println("Max Number of Features: " + maxFeatures)
    println("(Optional) Features: " + options.getOrElse('features, ""))
    println("(Optional) Additional values: " + fNames.mkString(","))
    println("(Optional) Additional values separator: " + valuesSep)
    println("(Optional) Term boosts file: " + options.getOrElse('boosts, ""))


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
    val (features, vectors) =
      if (vtype == "int")
        TFIDF.tfVectors(reader, field, docIds, words2, tfMode, termBoosts)
      else
        TFIDF.tfIdfVectors(reader, field, docIds, words2, tfMode, smthterm, idfMode, termBoosts)
    val idValues = if (idField.nonEmpty) fieldValues(reader, docIds, Seq(idField)).map(vals => vals(idField)(0)) else List.empty[String]
    dumpVectors(vectors, out, outputSep, idValues, words2.size, header)


    // output words
    val wordsFile = new File(wordsOut)
    for (output <- managed(new PrintWriter(new FileWriter(wordsFile)))) {
      features.zipWithIndex.foreach{case(word, id) => output.println((id + 1).toString + "," + word)}
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

  def dumpVectors(vectors: => Stream[Seq[Any]], out: String = "data.txt", outputSep: String, idValues: List[String], featureSize: Int, header: Boolean = false): Unit = {
    val file: File = new File(out)
    for(output <- managed(new BufferedWriter(new FileWriter(file)))) {
      @tailrec def loop(xs: Stream[Seq[Any]]): Unit = xs match {
        case Stream.Empty => ()
        case vec #:: tail => {
          output.write(vec.mkString(outputSep))
          output.newLine()
          loop(tail)
        }
      }
      if (idValues.isEmpty) {
        if (header) {
          output.write((1 to featureSize).mkString(outputSep))
          output.newLine()
        }
        loop(vectors)
      }
      else {
        if (header) {
          output.write(outputSep)
          output.write((1 to featureSize).mkString(outputSep))
          output.newLine()
        }
        idValues.zip(vectors).foreach{case (id, vec) => {
          output.write(id + outputSep)
          output.write(vec.mkString(outputSep))
          output.newLine()
        }}
      }

      output.flush()
    }
  }

}
