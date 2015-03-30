package org.nlp4l.core

import org.apache.lucene.index.{Terms => LuceneTerms}

/**
 * Class representing Term Vector for a document
 *
 * @constructor Create a new TermVector instance with Lucene Terms.
 *
 * @param tv the Terms instance. This must be the return value of IndexReader#getTermVector()
 */
class TermVector(tv: LuceneTerms) extends Map[String, Doc] {

  // enumerate all terms and docs with this term vector
  lazy val terms: Map[String, Doc] = {
    val te = tv.iterator(null)
    val builder = Map.newBuilder[String, Doc]
    var term = te.next()
    while (term != null) {
      val text = term.utf8ToString()
      val de = te.docs(null, null)
      if (de != null) de.nextDoc()
      val dpe = te.docsAndPositions(null, null)
      if (dpe != null) dpe.nextDoc()
      builder += (text -> new Doc(0, de.freq(), text, dpe))
      term = te.next
    }
    builder.result()
  }

  override def +[B1 >: Doc](kv: (String, B1)): Map[String, B1] = terms + kv

  override def get(key: String): Option[Doc] = terms.get(key)

  override def iterator: Iterator[(String, Doc)] = terms.iterator

  override def -(key: String): Map[String, Doc] = terms - key

  def hasPositions: Boolean = terms.exists(e => e._2.hasPositions)

  def hasOffsets: Boolean = terms.exists(e => e._2.hasOffsets)

}
