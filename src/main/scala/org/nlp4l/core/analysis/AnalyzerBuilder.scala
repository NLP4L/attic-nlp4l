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

import org.apache.lucene.analysis.custom.CustomAnalyzer
import org.apache.lucene.analysis.custom.CustomAnalyzer.Builder
import collection.JavaConversions._

/**
 * Factory for [[AnalyzerBuilder]] instances
 */
object AnalyzerBuilder {
  /**
   * Create a new AnalyzerBuilder instance.
   */
  def apply() = new AnalyzerBuilder()
}

/**
 * Class for building custom [[Analyzer]]s.
 * @see org.apache.lucene.analysis.custom.CustomAnalyzer.Builder
 */
class AnalyzerBuilder {
  val builder: Builder = CustomAnalyzer.builder

  def addCharFilter(name: String, params: String*): Unit = {
    builder.addCharFilter(name, params: _*)
  }

  def addCharFilter(name: String, params: Map[String, String]): Unit = {
    builder.addCharFilter(name, params)
  }

  def withTokenizer(name: String, params: String*): Unit = {
    builder.withTokenizer(name, params: _*)
  }

  def withTokenizer(name: String, params: Map[String, String]): Unit = {
    builder.withTokenizer(name, params)
  }

  def addTokenFilter(name: String, params: String*): Unit = {
    builder.addTokenFilter(name, params: _*)
  }

  def addTokenFilter(name: String, params: Map[String, String]): Unit = {
    builder.addTokenFilter(name, params)
  }

  def build(): Analyzer = {
    Analyzer(builder.build)
  }
}
