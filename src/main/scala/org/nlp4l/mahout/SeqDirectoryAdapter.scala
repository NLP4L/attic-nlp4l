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

package org.nlp4l.mahout

import java.io.{BufferedWriter, FileWriter, File}

import org.nlp4l.core.{IReader, SchemaLoader}
import org.nlp4l.stats.TFIDF
import org.nlp4l.util.{Adapter, FeatureSelector}

import resource._

object SeqDirectoryAdapter extends Adapter with  FeatureSelector {
  def main(args: Array[String]): Unit = {
    val usage =
      """
        |Usage:
        |SeqDirectoryAdapter
        |       -s <schema file>
        |       -f <feature field>
        |       -l <label field>
        |       [-o <output dir>]
        |       [--features <feature1>{,<feature2>}]
        |       [--maxDFPercent maxDFPercent]
        |       [--minDF minDF]
        |       [--maxFeatures maxFeatures]
        |       <index dir>
      """.stripMargin
    def parseOption(parsed: Map[Symbol, String], list: List[String]): Map[Symbol, String] = list match {
      case Nil => parsed
      case "-s" :: value :: tail => parseOption(parsed + ('schema -> value), tail)
      case "-f" :: value :: tail => parseOption(parsed + ('field -> value), tail)
      case "-l" :: value :: tail => parseOption(parsed + ('label -> value), tail)
      case "-o" :: value :: tail => parseOption(parsed + ('outdir -> value), tail)
      case "--features" :: value :: tail => parseOption(parsed + ('features -> value), tail)
      case value :: tail => parseOption(parsed + ('index -> value), tail)
    }
    val options = parseOption(Map(), args.toList) ++ parseCriteriaOption(Map(), args.toList)
    if (!List('index, 'schema, 'label, 'field).forall(options.contains)) {
      println(usage)
      System.exit(1)
    }

    val idxDir = options('index)
    val schemaFile = options('schema)
    val field = options('field)
    val labelField = options('label)
    val outdir = options.getOrElse('outdir, "msd-out")
    val words = if (options.contains('features)) readFeatures(options('features)) else Set.empty[String]
    val maxDFPercent = if (options.contains('maxDFPercent)) options('maxDFPercent).toDouble / 100.0 else 0.99
    val minDF = if (options.contains('minDF)) options('minDF).toInt else 1
    val maxFeatures = if (options.contains('maxFeatures)) options('maxFeatures).toInt else -1

    println("Index directory: " + idxDir)
    println("Schema file: " + schemaFile)
    println("Feature Field: " + field)
    println("Label Field: " + labelField)
    println("Output to: " + outdir)
    println("Max DF Percent: " + maxDFPercent)
    println("Min DF: " + minDF)
    println("Max Number of Features: " + maxFeatures)
    println("(Optional) Features: " + words.mkString(","))

    // create output dir if not exist
    val dir = new File(outdir)
    if (!dir.exists()) dir.mkdirs()

    val schema = SchemaLoader.loadFile(schemaFile)
    val reader = IReader(idxDir, schema)

    // select features (if not specified)
    val words2 =
      if (words.isEmpty)
        selectFeatures(reader, field, minDF, maxDFPercent, maxFeatures)
      else words

    val docIds = reader.universalset().toList
    val (features, vectors) = TFIDF.tfVectors(reader, field, docIds, words2)
    var labelMap = Map[String, Int]()
    docIds.zip(vectors).foreach{case(docId: Int, vec: Seq[Long]) => {
      reader.document(docId) match {
        case Some(doc) => {
          doc.getValue(labelField) match {
            case Some(vals) => {
              val label = vals(0)
              val labelDir = new File(dir, label)
              if (!labelDir.exists()) labelDir.mkdirs()
              val fileNum = labelMap.getOrElse(label, 0) + 1
              labelMap += (label -> fileNum)
              val file = new File(labelDir, fileNum.toString + ".txt")
              for (out <- managed(new BufferedWriter(new FileWriter(file)))) {
                features.zip(vec).foreach{case(word: String, count: Long) => {
                  if (count > 0) {
                    for (i <- 0 until count.toInt) {
                      out.write("%s ".format(word))
                    }
                    out.newLine()
                  }
                }}
              }
            }
            case _ => ()
          }
        }
        case _ => ()
      }
    }}
  }
}
