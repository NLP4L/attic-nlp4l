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

import collection.JavaConversions._
import org.apache.lucene.analysis.{ Analyzer => LuceneAnalyzer }
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.nlp4l.core.analysis.Analyzer

/**
 * Factory for [[Schema]] instances
 */
object Schema {
  /**
   * Create a new Schema instance with given default analyzer and field types mapping.
   * @param defaultAnalyzer the default analyzer
   * @param fieldTypes the field types mapping
   * @return new schema
   */
  def apply(defaultAnalyzer: Analyzer, fieldTypes: Map[String, FieldType]) = new Schema(defaultAnalyzer, fieldTypes)
}

/**
 * Class representing schema. This holds a default analyzer and a field types mapping.
 *
 * @constructor Create a new Schema instance with given default analyzer and field types mapping.
 *
 * @param defaultAnalyzer the default analyzer
 * @param fieldTypes the map of field's name and type.
 */
class Schema(val defaultAnalyzer: Analyzer, val fieldTypes: Map[String, FieldType]){

  /**
   * Generate a Lucene PerFieldAnalyzerWrapper instance from the default analyzer and field types mapping.
   */
  def perFieldAnalyzer(): LuceneAnalyzer = {
    new PerFieldAnalyzerWrapper(defaultAnalyzer.delegate, fieldTypes.mapValues(_.analyzer.delegate))
  }

  /**
   * Returns the field type for given field name.
   * @param name the field name
   * @return the field type or None if the requested field does not exist
   */
  def get(name: String): Option[FieldType] = {
    fieldTypes.get(name)
  }

  /**
   * Returns the analyzer for given field name.
   * @param name the field name
   * @return the analyzer or None if requested field does not exist
   */
  def getAnalyzer(name: String): Option[Analyzer] = get(name) match {
    case Some(fieldType) => if (fieldType.analyzer == null) None else Option(fieldType.analyzer)
    case _ => None
  }
}

/**
 * Case class representing field type
 *
 * @constructor Create a new FieldType instance.
 * @param analyzer the analyzer. If given null, the field value is recognized as String
 * @param indexed set true when the fields associated to this field type should be indexed
 * @param stored set true when the fields associated to this field type should be stored
 * @param termVectors set true when the fields associated to this field type should have a term vector
 * @param termPositions set true when the fields associated to this field type should have term positions with the term vector
 * @param termOffsets set true when the fields associated to this field type should have term offsets with the term vector
 */
// TODO: IntField, LongField, etc.
case class FieldType(analyzer: Analyzer, indexed: Boolean, stored: Boolean,
                     termVectors: Boolean = false,termPositions: Boolean = false, termOffsets: Boolean = false) {
}

/*
class Schema extends Map[String, FieldType] {

  val fieldTypes = scala.collection.mutable.Map[String, FieldType]()
  //val fieldTypes = scala.collection.immutable.Map[String, FieldType]()

  def perFieldAnalyzer(): LuceneAnalyzer = {
    new PerFieldAnalyzerWrapper(new StandardAnalyzer, fieldTypes.mapValues(_.analyzer.delegate))
  }

  // Members declared in scala.collection.immutable.Map
  def +[B1 >: FieldType](kv: (String, B1)): scala.collection.immutable.Map[String,B1] = {
    if(kv._2 != null){
      fieldTypes += kv._1 -> kv._2.asInstanceOf[FieldType]
    }
    Map() ++ fieldTypes
  }

  // Members declared in scala.collection.MapLike
  def -(key: String): scala.collection.immutable.Map[String,FieldType] = {
    fieldTypes -= key
    Map() ++ fieldTypes
  }

  def get(key: String): Option[FieldType] = {
    fieldTypes.get(key)
  }

  def iterator: Iterator[(String, FieldType)] = {
    fieldTypes.iterator
  }
}
*/
