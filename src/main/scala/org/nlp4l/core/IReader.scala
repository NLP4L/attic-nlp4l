package org.nlp4l.core

import java.nio.file.{FileSystems, Path}

import org.nlp4l.core.analysis.Analyzer

/**
 * Factory for [[IReader]] instances.
 */
object IReader {
  /**
   * Create a new IReader instance with given path and schema.
   * @param path the path for index directory
   * @param schema the schema
   * @return the new IReader instance
   */
  def apply(path: Path, schema: Schema) = new IReader(path, schema)

  /**
   * Create a new IReader instance with given path and schema.
   * @param index the path string for index directory
   * @param schema the schema
   * @return the new IReader instance
   */
  def apply(index: String, schema: Schema) = new IReader(FileSystems.getDefault.getPath(index), schema)
}

/**
 * Class representing schema-aware index reader. This is a [[RawReader]] with a [[Schema]] instance.
 *
 * @constructor Create a new IReader instance with given index path and schema.
 *
 * @param path the path for index directory
 * @param schema the schema
 */
class IReader(path: Path, schema: Schema) extends RawReader(path) {

  /**
   * Returns the field type for given field name.
   * @param fName the field name
   * @return the field type or None if the requested field does not exist
   */
  def getFieldType(fName: String): Option[FieldType] = schema.get(fName)

  /**
   * Returns the analyzer for given field name.
   * @param fName the field name
   * @return the analyzer or None if the requested field does not exist
   */
  def getAnalyzer(fName: String): Option[Analyzer] = schema.getAnalyzer(fName)

}
