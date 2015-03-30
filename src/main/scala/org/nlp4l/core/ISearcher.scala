package org.nlp4l.core

import java.nio.file.{FileSystems, Path}

import org.apache.lucene.search.{Filter => LuceneFilter}
import org.apache.lucene.search._

/**
 * Class representing a searcher for the index. This is a thin wrapper for Lucene IndexSearcher.
 *
 * @constructor Create a new ISearcher
 *
 * @param reader the RawReader instance
 */
// TODO: needs to hold a schema
class ISearcher(val reader: RawReader) {
  val is = new IndexSearcher(reader.ir)

  /**
   * Search thd documents with given query, filter, sort criteria.
   * @param query the Query instance
   * @param filter the Filter instance
   * @param rows the max number of documents to be returned
   * @param sort the Sort instance
   * @return the sequence of Document
   */
  // TODO: return org.nlp4l.core.Document instances instead of Lucene's Documents.
  def search(query: Query = new MatchAllDocsQuery(), filter: LuceneFilter = null, rows: Int = 10, sort: Sort = Sort.RELEVANCE): Seq[Document] =
    is.search(query, filter, rows, sort).scoreDocs.map(e => Document(e.doc, is.doc(e.doc)))

}

/**
 * Factory for [[ISearcher]] instances.
 */
object ISearcher {
  /**
   * Create a new [[ISearcher]] instance with given [[RawReader]].
   * @param reader the RawReader instance
   * @return a new ISearcher instance
   */
  def apply(reader: RawReader) = new ISearcher(reader)

  /**
   * Create a new [[ISearcher]] instance with given index path.
   * @param path the path for index path
   * @return a new ISearcher instance
   */
  def apply(path: Path): ISearcher = this.apply(RawReader(path))

  /**
   * Create a new [[ISearcher]] instance with given index path.
   * @param idxDir the path for index directory
   * @return a new ISearcher instance
   */
  def apply(idxDir: String): ISearcher = this.apply(RawReader(FileSystems.getDefault.getPath(idxDir)))
}
