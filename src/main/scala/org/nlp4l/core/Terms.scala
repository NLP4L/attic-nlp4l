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

import org.apache.lucene.index.{Terms => LuceneTerms, PostingsEnum, TermsEnum}
import org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS
import org.apache.lucene.util.{BytesRef, Bits}


/**
 * Class representing sequence of document ids (and optionally, positions/offsets information) associated to given field and a term.
 * This is a sequence of [[Doc]] instances. This holds Lucene PostingsEnum for the term internally.
 *
 * @constructor Create a new TermDocs instance with given term.
 *
 * @param text the string representation for the term
 * @param terms the Lucene Terms instance
 * @param liveDocs the Bits representing live docs
 * @param field the FieldInfo instance holding this term
 */
class TermDocs(val text: String, terms: LuceneTerms, liveDocs: Bits, field: FieldInfo) extends Seq[Doc] {

  val te = terms.iterator()
  val found = te.seekExact(new BytesRef(text))
  val (docFreq, totalTermFreq) =
  if (found)
    (te.docFreq(), te.totalTermFreq())
  else
    (0, 0L)

  def docStream: Stream[Doc] = {
    def newDoc(pe: PostingsEnum) = {
      // TODO: better ways to get the term vector for this doc?
      val tvTerm = {
        if (field == null || field.reader == null || field.reader.ir == null) null
        else
          field.reader.ir.getTermVector(pe.docID(), field.name)
      }
      new Doc(pe.docID(), pe.freq(), text, pe, tvTerm)
    }

    def from(first: Doc, pe: PostingsEnum): Stream[Doc] =
      if (pe.nextDoc() == NO_MORE_DOCS) first #:: Stream.empty
      else first #:: from(newDoc(pe), pe)

    lazy val pe =
      if (found) te.postings(null)
      else null

    if (pe == null || pe.nextDoc() == NO_MORE_DOCS) Stream.empty
    else from(newDoc(pe), pe)
  }

  /**
   * Returns the set of ids of documents including this term.
   */
  def docIds = docStream.map(_.docId).toSet

  override def iterator: Iterator[Doc] = docStream.iterator
  
  override def length: Int = docStream.length
  
  override def apply(idx: Int): Doc = docStream(idx)

  override def toString() = "Term(text=%s,docFreq=%d)".format(text, docFreq)

}


/**
 * Class representing a document in the index. This holds the Lucene document id and term frequency and optionally, positions/offsets information.
 *
 * @constructor Create a new Doc instance with given document id.
 *
 * @param docId the document id
 * @param freq the term frequency in this doc
 * @param text the term text
 * @param pe the Lucene's PostingsEnum instance
 * @param tvTerm the Lucene's Terms instance representing the term vector for this doc
 */
class Doc(val docId: Int, val freq: Int, text: String, pe: PostingsEnum = null, tvTerm: LuceneTerms = null) {

  // position & offsets info from term vector for this doc
  lazy val tvPosList: Iterable[PosAndOffset] =
    if (tvTerm == null) Iterable.empty[PosAndOffset]
    else {
      val builder = Seq.newBuilder[PosAndOffset]
      val te = tvTerm.iterator()
      if (te.seekExact(new BytesRef(text))) {
        val pe = te.postings(null)
        if (pe != null) {
          assert(pe.nextDoc() != NO_MORE_DOCS)  // term vector has exactly one document
          for (i <- 0 to pe.freq() - 1) {
            val pos = pe.nextPosition()
            val payload: String = if (pe.getPayload == null) null else pe.getPayload.utf8ToString()
            builder += PosAndOffset(pos, pe.startOffset(), pe.endOffset(), payload)
          }
        }
      }
      builder.result()
    }

  // populate position and offsets from index or term vector
  lazy val posAndOffsets: Seq[PosAndOffset] =
    // get positions info from term vector or PostingsEnum
    if (tvPosList.nonEmpty)
      tvPosList.toSeq
    else if (pe != null) {
      val builder = Seq.newBuilder[PosAndOffset]
      for (i <- 0 to pe.freq() - 1) {
        // next position
        val pos = pe.nextPosition()
        // offsets
        val (sOffset: Int, eOffset: Int): (Int, Int) =
          if (pe.startOffset() >= 0 && pe.endOffset() >= 0) (pe.startOffset(), pe.endOffset())
          else (-1, -1)
        // payload
        val payload: String = if (pe.getPayload == null) null else pe.getPayload.utf8ToString()

        builder += PosAndOffset(pos, sOffset, eOffset, payload)
      }
      builder.result()
    }
    else Seq.empty[PosAndOffset]

  /**
   * Returns true if this Doc has positions information else false.
   */
  def hasPositions: Boolean = posAndOffsets.nonEmpty

  /**
   * Returns true if this Doc has offsets information else false.
   */
  def hasOffsets: Boolean = hasPositions && posAndOffsets.exists(_.hasOffsets)

  /**
   * Returns true if this Doc has payload else false.
   */
  def hasPayloads: Boolean = hasPositions && posAndOffsets.exists(_.hasPayload)

  override def toString = "Doc(id=%d, freq=%d, positions=%s)".format(docId, freq, posAndOffsets)

}

/**
 * Case class representing a position of a term in a document. Optionally, this also holds start/end offsets and payload.
 */
case class PosAndOffset(pos: Int, startOffset: Int, endOffset: Int, payload: String) {

  /**
   * Returns true if this has offsets information
   */
  def hasOffsets = startOffset >= 0 && endOffset >= 0

  /**
   * Returns true if this has a payload
   */
  def hasPayload = payload != null

  override def toString = {
    if (hasOffsets) "(pos=%d,offset={%d-%d})".format(pos, startOffset, endOffset)
    else "pos=%d".format(pos)
  }

}

/*
 * Class representing terms associated to a field.
 * This is a sequence of [[TermDocs]] instances. This holds Lucene Terms internally.
 *
 * @constructor Create a new Terms instance with given Lucene's Terms instance.
 *
 * @param terms the Lucene Terms
 * @param liveDocs the Bits representing live docs
 * @param field the FieldInfo instance holding the terms
 */
class Terms(terms: LuceneTerms, liveDocs: Bits, val field: FieldInfo) extends Seq[TermDocs]{

  def termStream: Stream[TermDocs] = {
    def newTermDocs(te: TermsEnum) =
      new TermDocs(te.term().utf8ToString(), terms, liveDocs, field)

    def from(first: TermDocs, te: TermsEnum): Stream[TermDocs] =
      if (te.next() == null) first #:: Stream.empty
      else first #:: from(newTermDocs(te), te)

    val te = terms.iterator()
    if (te.next() == null) Stream.empty
    else from(newTermDocs(te), te)
  }

  override def iterator: Iterator[TermDocs] = termStream.iterator

  override def length: Int = termStream.length

  override def apply(idx: Int): TermDocs = termStream(idx)

  /**
   * Returns the number of unique terms for the field.
   */
  def uniqTerms = terms.size()

  /**
   * Returns the number of documents that have at least one term for this field, or -1 if this measure isn't stored by the codec
   */
  def docCount = terms.getDocCount

  /**
   * Returns string representation for the largest term (in lexicographic order) in the field
   */
  // TODO: support types other than String
  def max = terms.getMax.utf8ToString()

  /**
   * Returns string representation for the smallest term (in lexicographic order) in the field.
   */
  // TODO: support types other than String
  def min = terms.getMin.utf8ToString()

  /**
   * Returns the sum of document frequencies for all terms in this field, or -1 if this measure isn't stored by the codec.
   */
  def sumDocFreq = terms.getSumDocFreq

  /**
   * Returns the sum of total term frequencies for all terms in this field, or -1 if this measure isn't stored by the codec (or if this fields omits term freq and positions)
   */
  def sumTotalTermFreq = terms.getSumTotalTermFreq

  /**
   * Returns true if documents in this field store per-document term frequency
   */
  def hasFreqs = terms.hasFreqs

  /**
   * Returns true if documents in this field store offsets
   */
  def hasOffsets = terms.hasOffsets

  /**
   * Returns true if documents in this field store positions.
   */
  def hasPositions = terms.hasPositions

}
