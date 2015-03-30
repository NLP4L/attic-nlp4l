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

/**
 * Factory for [[Analyzer]] instances
 */
object Analyzer {
  /**
   * Create a new Analyzer instance with given Lucene Analyzer.
   * @param delegate the Lucene Analyzer instance to delegate analysis process
   * @return a new Analyzer instance
   */
  def apply(delegate: org.apache.lucene.analysis.Analyzer) =
    new Analyzer(delegate)
}

/**
 * Class for analyzing text. This is a thin wrapper for Lucene Analyzer.
 * @param delegate the Lucene Analyzer instance to delegate analysis process
 */
class Analyzer(val delegate: org.apache.lucene.analysis.Analyzer){

  /**
   * Analyze the given text and return produced tokens.
   * @param str the text to be analyzed
   * @return the new sequence of [[Token]] instances
   */
  def tokens(str: String): Seq[Token] = {
    val stream = delegate.tokenStream("", str)

    stream.reset
    val builder = Seq.newBuilder[Token]
    while (stream.incrementToken()) {
      val token = Token()
      stream.reflectWith(token)
      builder += token
    }
    stream.close
    builder.result
  }
}
