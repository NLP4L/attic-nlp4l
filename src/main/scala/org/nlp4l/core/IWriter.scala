package org.nlp4l.core

import java.nio.file.FileSystems
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

/**
 * Factory for [[IWriter]] instances
 */
object IWriter {
  /**
   * Create a new IWriter instance with given index path and [[Schema]].
   * @param index the path for index directory
   * @param schema the schema
   * @return a new IWriter instance
   */
  def apply(index: String, schema: Schema) = new IWriter(index, schema)
}

/**
 * Class representing schema aware index writer. This holds a Lucene IndexWriter internally.
 *
 * @constructor Create a new IWriter instance with given index path and schema.
 *
 * @param index the index path string
 * @param schema the schema
 */
class IWriter(index: String, schema: Schema) {

  val config : IndexWriterConfig = new IndexWriterConfig(schema.perFieldAnalyzer)
  val directory : Directory = FSDirectory.open(FileSystems.getDefault.getPath(index))
  val writer : IndexWriter = new IndexWriter(directory, config)

  /**
   * Add a document to the index.
   * @param document the document to add
   */
  def write(document: Document): Unit = {
    writer.addDocument(document.luceneDocument(schema))
  }

  /**
   * Close the index.
   *
   * NOTE: Before closing, this calls IndexWriter.forceMerge(1)
   */
  def close(): Unit = {
    writer.forceMerge(1)    // TODO: remove this
    writer.close
    directory.close
  }
}
