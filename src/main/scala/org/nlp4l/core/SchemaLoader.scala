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

package org.nlp4l.core

import com.typesafe.config.{ConfigObject, Config, ConfigFactory}
import org.apache.lucene.analysis.custom.CustomAnalyzer
import org.apache.lucene.analysis.{Analyzer => LuceneAnalyzer}
import org.nlp4l.core.analysis.{Analyzer}

import scala.collection.JavaConversions
import scala.collection.immutable.HashMap

/**
 * Object for loading schemas.
 */
object SchemaLoader {

  /**
   * Load a schema configuration from given resource.
   * @param resource the resource or file path to schema configuration
   * @return a new [[Schema]] instance
   */
  def load(resource : String): Schema = {
    val conf = ConfigFactory.load(resource)
    val schema = if (conf.hasPath("schema")) conf.getConfig("schema") else throw new InvalidSchemaException("No root object \"schema\".")
    // custom analyzer definitions
    val analyzers =
      if (schema.hasPath("analyzers")) JavaConversions.asScalaIterator(schema.getConfigList("analyzers").iterator).map(buildAnalyzer(_)).toMap
      else Map.empty[String, Analyzer]
    val defAnalyzer = if (schema.hasPath("defAnalyzer")) schema.getAnyRef("defAnalyzer") else throw new InvalidSchemaException("The path \"schema.defAnalyzer\" is mandatory.")
    val defANalyzerObj =
      if (defAnalyzer.isInstanceOf[String]) analyzers.get(defAnalyzer.asInstanceOf[String]) match {
        case Some(a) => a
        case _ => throw new InvalidSchemaException("Unknown analyzer name: " + defAnalyzer.asInstanceOf[String])
      } else {
        // build Analyzer in place
        buildAnalyzer(schema.getConfig("defAnalyzer"), false)._2
      }
    val fields = if (schema.hasPath("fields")) schema.getConfigList("fields") else throw new InvalidSchemaException("The path \"schema.fields\" is mandatery")
    if (fields.isEmpty) throw new InvalidSchemaException("\"schema.fields\" must have one or more field definitions")
    val fieldTypes = JavaConversions.asScalaIterator(fields.listIterator()).map(f => {
      val name = if (f.hasPath("name")) f.getString("name") else throw new InvalidSchemaException("The path \"schema.fields.[N].name\" is mandatory")
      val analyzer = if (f.hasPath("analyzer")) f.getAnyRef("analyzer") else null
      val indexed = if (f.hasPath("indexed")) f.getBoolean("indexed") else false
      val stored = if (f.hasPath("stored")) f.getBoolean("stored") else false
      val termVector = if (f.hasPath("termVector")) f.getBoolean("termVector") else false
      val termPosition = if (termVector && f.hasPath("positions")) f.getBoolean("positions") else false
      val termOffset = if (termVector && f.hasPath("offsets")) f.getBoolean("offsets") else false
      val analyzerObj =
      if (analyzer == null) {
        null
      } else if (analyzer.isInstanceOf[String]) analyzers.get(analyzer.asInstanceOf[String]) match {
        case Some(a) => a
        case _ => throw new InvalidSchemaException("Unknown analyzer name: " + defAnalyzer.asInstanceOf[String])
      } else {
        // build custom analyzer in place
        buildAnalyzer(f.getConfig("analyzer"), false)._2
      }
      (name, FieldType(analyzerObj, indexed, stored, termVector, termPosition, termOffset))
    }).toMap
    Schema(defANalyzerObj, fieldTypes)
  }

  private def buildAnalyzer(analyzer: Config, nameRequired: Boolean = true): (String, Analyzer) = {
    val name =
      if (analyzer.hasPath("name")) analyzer.getString("name")
      else if (!nameRequired) ""
      else throw new InvalidSchemaException("The path \"schema.analyzers.[N].name\" is mandatory.")

    if (analyzer.hasPath("class")) {
      // create a new Analyzer instance with no arguments.
      val analyzerObj = try {
        val clazz = Class.forName(analyzer.getString("class"))
        Analyzer(clazz.newInstance.asInstanceOf[LuceneAnalyzer])
      } catch {
        case e: Exception => throw e
      }
      (name, analyzerObj)
    } else {
      // create a new Analyzer instance with CustomAnalyzer.Builder
      // parse the Tokenizer settings. This is mandatory.
      val tokenizer = if (analyzer.hasPath("tokenizer")) analyzer.getConfig("tokenizer") else throw new InvalidSchemaException("The path \"schema.analyzers.[N].tokenizer\" is mandatory.")
      val tokenizerClazz = if (tokenizer.hasPath("factory")) tokenizer.getString("factory") else throw new InvalidSchemaException("The path \"schema.analyzers.[N].tokenizer.factory\" is mandatory.")
      val tokenizerParams: Map[String, String] =
        if (tokenizer.hasPath("params")) JavaConversions.asScalaIterator(tokenizer.getConfigList("params").iterator).map { c => (c.getString("name"), c.getString("value")) }.toMap
        else Map.empty[String, String]
      // parse settings for CharFilters. This is not mandatory or can be empty.
      val charFilters = if (analyzer.hasPath("char_filters")) JavaConversions.asScalaIterator(analyzer.getConfigList("char_filters").iterator) else List.empty[Config]
      // parse setting for TokenFilters. This is not mandatory or can be empty.
      val tokenFilters = if (analyzer.hasPath("filters")) JavaConversions.asScalaIterator(analyzer.getConfigList("filters").iterator) else List.empty[Config]

      // build the Analyzer instance with CumsomAnalyzer.Builder
      // TODO: more readable...
      val builder = CustomAnalyzer.builder
      val jParams = JavaConversions.mapAsJavaMap(collection.mutable.HashMap.empty ++ tokenizerParams)
      builder.withTokenizer(tokenizerClazz, jParams)
      charFilters.foreach(filter => {
        val factory = if (filter.hasPath("factory")) filter.getString("factory") else throw new InvalidSchemaException("The path \"schema.analyzers.[N].char_filters.[N].factory\" is mandatory.")
        val params = if (filter.hasPath("params")) JavaConversions.asScalaIterator(filter.getConfigList("params").iterator).map { c => (c.getString("name"), c.getString("value")) }.toMap
        else Map.empty[String, String]
        val jParams = JavaConversions.mapAsJavaMap(collection.mutable.HashMap.empty ++ params)
        builder.addCharFilter(factory, jParams)
      })
      tokenFilters.foreach(filter => {
        val factory = if (filter.hasPath("factory")) filter.getString("factory") else throw new InvalidSchemaException("The path \"schema.analyzers.[N].char_filters.[N].factory\" is mandatory.")
        val params: Map[String, String] =
          if (filter.hasPath("params")) JavaConversions.asScalaIterator(filter.getConfigList("params").iterator).map { c => (c.getString("name"), c.getString("value")) }.toMap
          else Map.empty[String, String]
        val mParams = collection.mutable.HashMap.empty ++ params
        val jParams = JavaConversions.mapAsJavaMap(collection.mutable.HashMap.empty ++ params)
        builder.addTokenFilter(factory, jParams)
      })
      (name, Analyzer(builder.build))
    }
  }
}

class InvalidSchemaException(msg: String) extends RuntimeException(msg)



