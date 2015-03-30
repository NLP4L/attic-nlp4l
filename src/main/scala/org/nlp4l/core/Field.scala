package org.nlp4l.core

import org.apache.lucene.document.{ Field => LuceneField }
import org.apache.lucene.document.{ FieldType => LuceneFieldType }
import org.apache.lucene.index._

/**
 * Factory for [[Field]] instances.
 */
object Field {
  /**
   * Create a new Field instance with given name and values.
   * @param name the field name
   * @param values the field values
   * @return a new Field instance
   */
  def apply(name: String, values: List[String]) = new Field(name, values)

  /**
   * Create a new Field instance with given name and a value.
   * @param name the field name
   * @param value the field value
   * @return a new Field instance
   */
  def apply(name: String, value: String) = new Field(name, List(value))
}

/**
 * Class representing a field attached to a [[Document]]. This holds a name and one or more values.
 *
 * @constructor Create a new Field instance with given name and values.
 *
 * @param name the field name
 * @param values the field values
 */
class Field(val name: String, val values: List[String]){

  override def toString = "Field(name=%s,values=%s)".format(name, values)

  /**
   * Generate a new list of Lucene Field instances with given field type.
   * @param fieldType the field type
   * @param name the field value
   * @return the list of lucene fields
   */
  def luceneFields(fieldType: FieldType, name: String): List[IndexableField] =
    values.map(v => new LuceneField(name, v, type2luceneType(fieldType)))

  private def type2luceneType(fieldType: FieldType) = {
    val lft: LuceneFieldType = new LuceneFieldType
    if (fieldType.indexed) {
      // TODO: support DOCS, DOCS_AND_FREQS, DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS
      lft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)
      lft.setStoreTermVectors(fieldType.termVectors || fieldType.termOffsets || fieldType.termPositions)
      lft.setStoreTermVectorOffsets(fieldType.termOffsets)
      lft.setStoreTermVectorPositions(fieldType.termPositions)
    }
    else {
      lft.setIndexOptions(IndexOptions.NONE)
    }
    lft.setStored(fieldType.stored)
    lft.setTokenized(fieldType.analyzer != null)
    lft
  }
}
