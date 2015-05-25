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

import org.apache.lucene.index.{Terms => LuceneTerms, FieldInfo => LuceneFieldInfo, TermsEnum}
import org.apache.lucene.util.Bits

/**
 * Class representing a field. This holds Lucene FieldInfo and Terms for the field internally.
 *
 * @constructor Creates a new FieldInfo instance
 *
 * @param info the Lucene FieldInfo instance
 * @param luceneTerms the Lucene Terms instance
 * @param liveDocs the Bits representing live docs
 * @param reader the [[RawReader]] instance
 */
class FieldInfo(info: LuceneFieldInfo, luceneTerms: LuceneTerms, liveDocs: Bits, val reader: RawReader) {

  val this.info = info

  //lazy val terms = new Terms(luceneTerms, liveDocs, this)

  private def termStream: Stream[TermDocs] = {
    def newTermDocs(te: TermsEnum) =
      new TermDocs(te.term().utf8ToString(), luceneTerms, liveDocs, this)

    def from(first: TermDocs, te: TermsEnum): Stream[TermDocs] =
      if (te.next() == null) first #:: Stream.empty
      else first #:: from(newTermDocs(te), te)

    val te = luceneTerms.iterator(null)
    if (te.next() == null) Stream.empty
    else from(newTermDocs(te), te)
  }

  /**
   * Returns terms and associated docs information this field contains
   * @return the Iterable for [[TermDocs]]
   */
  def terms = termStream

  /**
   * Returns the field name
   */
  def name = info.name

  /**
   * Returns the Lucene internal field number
   */
  def number = info.number

  //def attributes = info.attributes()

  //def attribute(key: String) = info.getAttribute(key)

  //def docValuesGen = info.getDocValuesGen()

  // TODO: wrap return object
  //def docValuesType = info.getDocValuesType()

  /**
   * Returns the IndexOption instance for this field
   */
  // TODO: wrap return object
  def indexOptions = info.getIndexOptions()

  /**
   * Returns true if this field actually has any norms.
   */
  def hasNorms = info.hasNorms()

  /**
   * Returns true if any payloads exist for this field.
   */
  def hasPayloads = info.hasPayloads()

  /**
   * Returns true if any term vectors exist for this field.
   */
  def hasVectors = info.hasVectors()

  /**
   * Returns true if norms are explicitly omitted for this field.
   */
  def omitNorms = info.omitsNorms()

  /**
   * Returns the number of unique terms for the field.
   */
  def uniqTerms = luceneTerms.size()

  /**
   * Returns the number of documents that have at least one term for this field, or -1 if this measure isn't stored by the codec
   */
  def docCount = luceneTerms.getDocCount

  /**
   * Returns string representation for the largest term (in lexicographic order) in the field
   */
  // TODO: support types other than String
  def maxTerm = luceneTerms.getMax.utf8ToString()

  /**
   * Returns string representation for the smallest term (in lexicographic order) in the field.
   */
  // TODO: support types other than String
  def minTerm = luceneTerms.getMin.utf8ToString()

  /**
   * Returns the sum of document frequencies for all terms in this field, or -1 if this measure isn't stored by the codec.
   */
  def sumDocFreq = luceneTerms.getSumDocFreq

  /**
   * Returns the sum of total term frequencies for all terms in this field, or -1 if this measure isn't stored by the codec (or if this fields omits term freq and positions)
   */
  def sumTotalTermFreq = luceneTerms.getSumDocFreq

  /**
   * Returns true if documents in this field store per-document term frequency
   */
  def hasFreqs = luceneTerms.hasFreqs

  /**
   * Returns true if documents in this field store offsets
   */
  def hasOffsets = luceneTerms.hasOffsets

  /**
   * Returns true if documents in this field store positions.
   */
  def hasPositions = luceneTerms.hasPositions

  /**
   * Returns the [[TermDocs]] instance with given string representation.
   * @param text the string representation for the term
   * @return the TermDocs or None if this term does not exist
   */
  def term(text: String): Option[TermDocs] = termStream.find(_.text == text)

  override def toString = "Field(#%d,name=%s))".format(number, name)

}