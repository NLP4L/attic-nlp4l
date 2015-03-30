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

import java.nio.file.{Path, FileSystems}
import java.util.Comparator

import org.apache.lucene.index.{SlowCompositeReaderWrapper, DirectoryReader, Term}
import org.apache.lucene.misc.{HighFreqTerms, TermStats}
import org.apache.lucene.store.FSDirectory

/**
 * Class representing a index reader. This holds a Lucene LeafReader internally.
 *
 * @constructor Create a new RawReader instance with given index path.
 *
 * @param path the path for index directory
 */
class RawReader(val path: Path) {
  val dr = DirectoryReader.open(FSDirectory.open(path))

  val ir = SlowCompositeReaderWrapper.wrap(dr)

  lazy val liveDocs = ir.getLiveDocs()

  lazy val fieldMap = {
    val mapBuilder = Map.newBuilder[String, FieldInfo]
    val it = ir.getFieldInfos.iterator()
    while (it.hasNext) {
      val info = it.next()
      val field = new FieldInfo(info, ir.fields.terms(info.name), liveDocs, this)
      mapBuilder += (field.name -> field)
    }
    mapBuilder.result
  }

  lazy val numFields = ir.fields().size()

  def fields = fieldMap.values.toSeq

  def fieldNames = fieldMap.keys.toSeq

  var closed = false

  /**
   * Close the index.
   */
  def close: Unit = {
    if (!closed)
      ir.close()
      closed = true
  }

  /**
   * Returns the number of documents containing the term in the given field.
   * @param fName the field name
   * @param text the string representation for the term
   */
  def docFreq(fName: String, text: String): Int = ir.docFreq(new Term(fName, text))

  /**
   * Returns the [[Document]] instance for the given document id.
   * @param docId the Lucene's internal document id
   * @return the Document or None if the corresponding document does not exist
   */
  def document(docId: Int): Option[Document] =
    if (docId < 0 || docId > maxDoc) None
    else Option(Document(docId, ir.document(docId)))

  // TODO: wrap return object
  //def binaryDocValues(fName: String): BinaryDocValues = ir.getBinaryDocValues(fName)

  /**
   * Returns the number of documents that have at least one term for this field, or -1 if this measure isn't stored by the codec.
   * @param fName the field name
   */
  def docCount(fName: String): Long = ir.getDocCount(fName)

  // TODO: wrap return object
  //def normValues(fName: String): NumericDocValues = ir.getNormValues(fName)

  // TODO: wrap return object
  //def numericDocValues(fName: String): NumericDocValues = ir.getNumericDocValues(fName)

  // TODO: wrap return object
  //def sortedDocValues(fName: String): SortedDocValues = ir.getSortedDocValues(fName)

  // TODO: wrap return object
  //def sortedNumericDocValues(fName: String): SortedNumericDocValues = ir.getSortedNumericDocValues(fName)

  // TODO: wrap return object
  //def sortedSetDocValues(fName: String): SortedSetDocValues = ir.getSortedSetDocValues(fName)

  /**
   * Returns the sum of document frequency for all terms in this field, or -1 if this measure isn't stored by the codec.
   * @param fName the field name
   */
  def sumDocFreq(fName: String): Long = ir.getSumDocFreq(fName)

  /**
   * Returns the sum of total term frequencies for all terms in this field, or -1 if this measure isn't stored by the codec (or if this fields omits term freq and positions).
   * @param fName the field name
   */
  def sumTotalTermFreq(fName: String): Long = ir.getSumTotalTermFreq(fName)

  /**
   * Returns the [[TermVector]] for the given document id and field name.
   * @param docId the document id
   * @param fName the field name
   * @return a new TermVector instance or None if the term vector is not stored in the index
   */
  def getTermVector(docId: Int, fName: String): Option[TermVector] = {
    val tv = ir.getTermVector(docId, fName)
    if (tv != null) Option(new TermVector(tv)) else None
  }

  /**
   * Returns true if any documents have been deleted.
   */
  def hasDeletions: Boolean = ir.hasDeletions()

  /**
   * Returns one greater than the largest possible document number.
   */
  def maxDoc: Int = ir.maxDoc()

  /**
   * Returns the number of deleted documents.
   */
  def numDeletedDocs: Int = ir.numDeletedDocs()

  /**
   * Returns the number of documents in this index.
   */
  def numDocs: Int = ir.numDocs()

  /**
   * Returns the total number of occurrences of term across all documents.
   * @param fName the field name
   * @param text the string representation for the term
   */
  def totalTermFreq(fName: String, text: String): Long = ir.totalTermFreq(new Term(fName, text))

  /**
   * Returns the [[TermDocs]] instance holding document ids (and optionally, positions/offsets information) associated to given field and term.
   * @param fName the field name
   * @param text the string representation for the term.
   * @return the [[TermDocs]] instance or None if the corresponding term does not exist
   */
  def termDocs(fName: String, text: String): Option[TermDocs] = field(fName) match {
    case Some(field) => field.term(text)
    case _ => None
  }

  /**
   * Returns the [[FieldInfo]] instance holding information of the field with given field name.
   * @param fName the field name
   * @return the [[FieldInfo]] instance or None if the corresponding field does not exist
   */
  def field(fName: String): Option[FieldInfo] = fieldMap.get(fName)

  /**
   * Returns top high frequent terms (based document frequencies) and associated frequencies.
   * @param field the field name
   * @param numTerms max terms to be returned
   * @return the Tuple of the term, document frequency, total term frequency associated to this term)
   */
  def topTermsByDocFreq(field: String, numTerms: Int = 20): Seq[(String, Int, Long)] = highFreqTerms(field, numTerms, new Comparator[TermStats] {
    override def compare(o1: TermStats, o2: TermStats): Int = {
      if (o1.docFreq < o2.docFreq)
        -1
      else if (o1.docFreq > o2.docFreq)
        1
      else
        0

    }
  })

  /**
   * Returns top high frequent terms (based total term frequencies) and associated frequencies.
   * @param field the field name
   * @param numTerms max terms to be returned
   * @return the Tuple of the term, document frequency, total term frequency associated to this term)
   */
  def topTermsByTotalTermFreq(field: String, numTerms: Int = 20): Seq[(String, Int, Long)] = highFreqTerms(field, numTerms, new Comparator[TermStats] {
    override def compare(o1: TermStats, o2: TermStats): Int = {
      if (o1.totalTermFreq < o2.totalTermFreq)
        -1
      else if (o1.totalTermFreq > o2.totalTermFreq)
        1
      else
        0

    }
  })

  private def highFreqTerms(field: String, numTerms: Int, comparator: Comparator[TermStats]): Seq[(String, Int, Long)] = {
    HighFreqTerms.getHighFreqTerms(ir, numTerms, field, comparator)
      .map(s => (s.termtext.utf8ToString(), s.docFreq, s.totalTermFreq))
  }

  /**
   * Returns index subset with given filter.
   * @param filter the Filter instance
   * @return a new DocSet instance
   */
  def subset(filter: Filter): DocSet =
    DocSet(filter.luceneFilter.getDocIdSet(ir.getContext, liveDocs).iterator())

  /**
   * Returns whole index as DocSet.
   * @return a new DocSet instance holding all document ids in the index
   */
  def universalset(): DocSet = subset(AllDocsFilter())

  override def toString() = "IndexReader(path='%s',closed=%s)".format(path, closed)
}

/**
 * Factory for [[RawReader]] instances
 */
object RawReader {

  /**
   * Create a RawReader instance with given index path
   * @param path the path for index directory
   * @return a new RawReader instance
   */
  def apply(path: Path) = new RawReader(path)

  /**
   * Create a RawReader instance with given index path
   * @param indexDir the path for index directory
   * @return a new RawReader instance
   */
  def apply(indexDir: String) = new RawReader(FileSystems.getDefault.getPath(indexDir))
}