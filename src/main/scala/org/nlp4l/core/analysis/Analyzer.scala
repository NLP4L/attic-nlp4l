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
