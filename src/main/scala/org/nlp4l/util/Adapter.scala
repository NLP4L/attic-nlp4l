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

package org.nlp4l.util

import java.io.{FileReader, BufferedReader, File}

import org.nlp4l.core.RawReader
import resource._

trait Adapter {
  def parseCommonOption(parsed: Map[Symbol, String], list: List[String]): Map[Symbol, String] = list match {
    case Nil => parsed
    case "-s" :: value :: tail => parseCommonOption(parsed + ('schema -> value), tail)
    case "-f" :: value :: tail => parseCommonOption(parsed + ('field -> value), tail)
    case "--type" :: value :: tail => parseCommonOption(parsed + ('type -> value), tail)
    case "--tfmode" :: value :: tail => parseCommonOption(parsed + ('tfmode -> value), tail)
    case "--smthterm" :: value :: tail => parseCommonOption(parsed + ('smthterm -> value), tail)
    case "--idfmode" :: value :: tail => parseCommonOption(parsed + ('idfmode -> value), tail)
    case "-o" :: value :: tail => parseCommonOption(parsed + ('outdir -> value), tail)
    case "--features" :: value :: tail => parseCommonOption(parsed + ('features -> value), tail)
    case "--outputSep" :: value :: tail => parseCommonOption(parsed + ('outputSep -> value), tail)
    case "--values" :: value :: tail => parseCommonOption(parsed + ('values -> value), tail)
    case "--valuesSep" :: value :: tail => parseCommonOption(parsed + ('valuesSep -> value), tail)
    case "--boosts" :: value :: tail => parseCommonOption(parsed + ('boosts -> value), tail)
    case value :: tail => parseCommonOption(parsed + ('index -> value), tail)
  }

  def fieldValues(reader: RawReader, docIds: List[Int], fields: Seq[String]): List[Map[String, List[String]]] = {
    docIds.map(id => reader.document(id) match {
      case Some(doc) => {
        fields.map(f => (f, doc.getValue(f).getOrElse(List.empty))).toMap
      }
      case _ => Map.empty[String, List[String]]
    })
  }

  def readFeatures(featureFile: String): Set[String] = {
    val file = new File(featureFile)
    // build word set from feature file
    val builder = Set.newBuilder[String]
    if (file.exists()) {
      for (input <- managed(new BufferedReader(new FileReader(file)))) {
        def read(): Unit = input.readLine() match {
          case null => ()
          case line => {
            builder += (line.trim)
            read()
          }
        }
        read()
      }
    }
    builder.result()
  }

  def readTermBoosts(boostFile: String): Map[String, Double] = {
    val file = new File(boostFile)
    // build term boosts map
    val builder = Map.newBuilder[String, Double]
    if (file.exists()) {
      for (input <- managed(new BufferedReader(new FileReader(file)))) {
        def read(): Unit = input.readLine() match {
          case null => ()
          case line => {
            val cols = line.split(",")
            builder += (cols(0).trim -> cols(1).trim.toDouble)
            read()
          }
        }
        read()
      }
    }
    builder.result()
  }
}
