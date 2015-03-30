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
