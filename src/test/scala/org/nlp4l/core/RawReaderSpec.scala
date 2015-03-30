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

import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.Term
import org.apache.lucene.search.{TermQuery, QueryWrapperFilter}
import org.nlp4l.core.analysis.Analyzer
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.util.Try
import scalax.file.Path

class RawReaderSpec extends FlatSpec with BeforeAndAfterAll{
  val indexDir = "/tmp/testindex_rawreaderspec"
  val contentAnalyzer = Analyzer(new StandardAnalyzer)
  val fieldTypes = Map("id" -> FieldType(null, true, true), "title" -> FieldType(null, true, true), "content" -> FieldType(contentAnalyzer, true, true, termVectors = true, termPositions = true, termOffsets = true))
  val schema = Schema(Analyzer(new KeywordAnalyzer), fieldTypes)
  val docs = List(
    Document(Set(Field("id", "0"), Field("title", "London Bridge"), Field("content", "London Bridge is falling down, Falling down, Falling down. London Bridge is falling down, My fair lady."))),
    Document(Set(Field("id", "1"), Field("title", "London Bridge"), Field("content", "Take a key and lock her up, Lock her up, Lock her up. Take a key and lock her up, My fair lady. "))),
    Document(Set(Field("id", "2"), Field("title", "London Bridge"), Field("content", "How will we build it up, Build it up, Build it up?　How will we build it up, My fair lady?"))),
    Document(Set(Field("id", "3"), Field("title", "London Bridge"), Field("content", "Build it up with silver and gold, Silver and gold, Silver and gold.　Build it up with silver and gold, My fair lady."))),
    Document(Set(Field("id", "4"), Field("title", "London Bridge"), Field("content", "Gold and silver I have none, I have none, I have none. Gold and silver I have none,　My fair lady."))),
    Document(Set(Field("id", "5"), Field("title", "London Bridge"), Field("content", "Build it up with needles and pins,　Needles and pins, Needles and pins. Build it up with needles and pins, My fair lady."))),
    Document(Set(Field("id", "6"), Field("title", "London Bridge"), Field("content", "Pins and needles bend and break, Bend and break,　Bend and break.　Pins and needles bend and break,　My fair lady."))),
    Document(Set(Field("id", "7"), Field("title", "London Bridge"), Field("content", "Build it up with wood and clay, Wood and clay, Wood and clay. Build it up with wood and clay, My fair lady."))),
    Document(Set(Field("id", "8"), Field("title", "London Bridge"), Field("content", "Wood and clay will wash away,　Wash away, Wash away.　Wood and clay will wash away, My fair lady."))),
    Document(Set(Field("id", "9"), Field("title", "London Bridge"), Field("content", "Build it up with stone so strong, Stone so strong, Stone so strong. Build it up with stone so strong, My fair lady."))),
    Document(Set(Field("id", "10"), Field("title", "London Bridge"), Field("content", "Stone so strong will last so long, Last so long, Last so long. Stone so strong will last so long, My fair lady.")))
  )

  override def beforeAll {
    deleteIndexDir()

    val iw1 = IWriter(indexDir, schema)
    docs.foreach{ iw1.write(_) }
    iw1.close()

    val iw2 = IWriter(indexDir, schema)
    iw2.writer.deleteDocuments(new Term("id", "3"))
    iw2.writer.deleteDocuments(new Term("id", "4"))
    iw2.writer.close()  // not optimize for test
  }

  override def afterAll {
    deleteIndexDir()
  }

  private def deleteIndexDir(): Unit = {
    val path = Path.fromString(indexDir)
    Try(path.deleteRecursively(continueOnFailure = false))
  }

  "A RawReader" can "open lucene index" in {
    val reader = RawReader(indexDir)
    assert(!reader.closed)
    assert(reader.path.toString == indexDir)
    reader.close
    assert(reader.closed)
  }

  it should "return index statistics" in {
    val reader = RawReader(indexDir)
    assert(reader.hasDeletions)
    assertResult(11)(reader.maxDoc)
    assertResult(9)(reader.numDocs)
    assertResult(2)(reader.numDeletedDocs)

    assert(reader.docCount("content") > 0)
    assert(reader.sumDocFreq("content") > 0)
    assert(reader.sumTotalTermFreq("content") > 0)

    assert(reader.docFreq("content", "london") > 0)
    assert(reader.totalTermFreq("content", "london") > 0)

    assert(reader.topTermsByDocFreq("content", 10).length > 0)
    assert(reader.topTermsByTotalTermFreq("content", 10).length > 0)
  }
  
  it should "return fields info" in {
    val reader = RawReader(indexDir)
    assertResult(3)(reader.numFields)
    assertResult(Vector("id", "title", "content"))(reader.fieldNames)
  }
  
  it should "return a field info" in {
    val reader = RawReader(indexDir)
    val field = reader.field("title").get
    assertResult("title")(field.name)
  }
  
  it should "return None if the specified field dose not exist" in {
    val reader = RawReader(indexDir)
    assert(reader.field("unknown") == None)
  }

  it should "should return terms and docs info" in {
    val reader = RawReader(indexDir)
    assert(reader.termDocs("content", "london").get.isInstanceOf[TermDocs])
  }

  it should "return a Document if this exists" in {
    val reader = RawReader(indexDir)
    assert(reader.document(1).isInstanceOf[Some[Document]])
  }

  it should "return None if the specified docid does not exist" in {
    val reader = RawReader(indexDir)
    assert(reader.document(100) == None)
  }

  it should "return term vector if term vector indexed" in {
    val reader = RawReader(indexDir)
    assert(reader.getTermVector(1, "content").nonEmpty)
  }

  it should "return None if term vector not indexed" in {
    val reader = RawReader(indexDir)
    assert(reader.getTermVector(1, "title") == None)
  }

  it should "return index subset (DocSet)" in {
    val reader = RawReader(indexDir)
    //val filter = new QueryWrapperFilter(new TermQuery(new Term("content", "stone")))
    val subDocs = reader.subset(TermFilter("content", "stone"))
    assertResult(Set(9,10))(subDocs.toSet)
  }

  it should "return index universalset (DocSet)" in {
    val reader = RawReader(indexDir)
    val allDocs = reader.universalset()
    assertResult(Set(0,1,2,5,6,7,8,9,10))(allDocs.toSet)
  }

  "A FieldInfo" should "return field information" in {
    val titleField = RawReader(indexDir).field("title").get
    assertResult("title")(titleField.name)
    assertResult(1)(titleField.number)
    assert(titleField.hasNorms)
    assert(!titleField.hasVectors)
    assert(titleField.uniqTerms == 1)
    assert(titleField.docCount == 11)
    assert(titleField.hasFreqs)
    assert(titleField.hasPositions)
    assert(!titleField.hasOffsets)
    
    val contentField = RawReader(indexDir).field("content").get
    assertResult(2)(contentField.number)
    assert(contentField.hasNorms)
    assert(contentField.hasVectors)
    assert(contentField.terms.size > 0)
    assert(contentField.docCount == 11)
    assert(contentField.hasFreqs)
    assert(contentField.hasPositions)
    assert(!contentField.hasOffsets)
  }
  
  it should "return a term info" in {
    val field = RawReader(indexDir).field("content").get
    val term = field.term("lady").get
    assertResult("lady")(term.text)
  }

  it should "return None if specified term does not exist" in {
    val field = RawReader(indexDir).field("content").get
    assert(field.term("foo") == None)
  }
  
  "A TermDocs" should "be a sequence of doc ids with (optionally) positions and offsets" in {
    val iw = IWriter(indexDir, schema)
    iw.writer.forceMerge(1) // optimize to purge words in deleted docs from index
    iw.close

    val field = RawReader(indexDir).field("title").get
    val termDocs = field.term("London Bridge").get
    assert(termDocs.size == 9)

    val field2 = RawReader(indexDir).field("content").get
    val termDocs2 = field2.term("build").get
    assertResult(4)(termDocs2.docFreq)
    assertResult(10)(termDocs2.totalTermFreq)

    val d0 = termDocs2(0)
    assert(d0.hasPositions)
    val expected = Seq(
      PosAndOffset(pos = 3, startOffset = 12, endOffset = 17, null),
      PosAndOffset(pos = 6, startOffset = 25, endOffset = 30, null),
      PosAndOffset(pos = 9, startOffset = 38, endOffset = 43, null),
      PosAndOffset(pos = 15, startOffset = 63, endOffset = 68, null))
    assertResult(expected)(d0.posAndOffsets)
  }
  
}
