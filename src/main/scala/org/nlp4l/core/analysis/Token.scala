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

package org.nlp4l.core.analysis

import org.apache.lucene.util.Attribute
import org.apache.lucene.util.AttributeReflector

object Token {
  def apply() = new Token
}

class Token extends Map[String, String] with AttributeReflector {

  val attributes = scala.collection.mutable.Map[String, String]()

  def reflect(attClass: Class[_ <: Attribute], key: String, value: Any): Unit = {
    if(value != null){
      attributes += key -> value.toString
    }
  }

  override def +[B1 >: String](kv: (String, B1)): Map[String, B1] = {
    if(kv._2 != null){
      attributes += kv._1 -> kv._2.toString
    }
    Map() ++ attributes
  }

  override def get(key: String): Option[String] = {
    attributes.get(key)
  }

  override def iterator: Iterator[(String, String)] = {
    attributes.iterator
  }

  override def -(key: String): Map[String, String] = {
    attributes -= key
    Map() ++ attributes
  }
}
