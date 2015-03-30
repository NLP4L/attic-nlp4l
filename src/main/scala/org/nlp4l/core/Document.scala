package org.nlp4l.core

import org.apache.lucene.document.{ Document => LuceneDocument }
import scala.collection.JavaConversions

/**
 * Factory for [[Document]] instances.
 */
object Document {
  /**
   * Create a new Document instance with given fields.
   * @param fields the field set
   * @return a new Document instance
   */
  def apply(fields: Set[Field]) = new Document(fields)

  /**
   * Create a new Document instance with given Lucene Document
   * @param docId the Lucene document id
   * @param ldoc the Lucene document
   * @return a new Document instance
   */
  def apply(docId: Int, ldoc: LuceneDocument): Document = {
     val fields = JavaConversions.collectionAsScalaIterable(ldoc.getFields)
        .foldLeft(scala.collection.mutable.Map.empty[String, List[String]])((m, f) => m += (f.name -> (f.stringValue() :: m.getOrElse(f.name, List.empty[String]))))
        .map(e => new Field(e._1, e._2)).toSet[Field]
    new Document(fields, docId)
  }
}

/**
 * Class representing a document. This holds one or more fields.
 *
 * @constructor Create a new Document instance with set of [[Field]]s and optionally, the corresponding Lucene document id.
 *
 * @param fields the field set
 * @param docId the Lucene document id
 */
class Document(val fields: Set[Field], val docId: Int = -1){
  val map = fields.map(e => (e.name, e)).toMap

  override def toString = "Document(docId=%s,fields=%s)".format(docId, fields.mkString("[",",","]"))

  /**
   * Generate an Lucene Document instance from this document with given schema.
   * @param schema the schema holding field type mapping
   * @return a new Lucene Document instance
   */
  def luceneDocument(schema: Schema): LuceneDocument = {
    val doc: LuceneDocument = new LuceneDocument
     for ((name, field) <- map) {
      schema.get(name) match {
        case Some(fieldType) => field.luceneFields(fieldType, name).foreach(doc.add)
        // TODO for now, if FieldType not available for the field, this is skipped silently. Or raise an error?
        case _ => Unit
      }
    }
    doc
  }

  /**
   * Returns the [[Field]] instance for given field name.
   * @param fName the field name
   * @return the field or None if the requested field does not exist.
   */
  def get(fName: String): Option[Field] = fields.find(_.name == fName)

  /**
   * Returns the string representation for the value of given field name.
   * @param fName the field name
   * @return the field values or None if the requested field does not exist.
   */
  def getValue(fName: String): Option[List[String]] = get(fName) match {
    case Some(field) => Option(field.values)
    case _ => None
  }
}