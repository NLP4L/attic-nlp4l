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
   * Delete all documents in the index.
   */
  def deleteAll(): Unit = {
    writer.deleteAll()
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
