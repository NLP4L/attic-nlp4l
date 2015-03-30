package org.nlp4l.core

import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.Term
import org.apache.lucene.search.{TermQuery, QueryWrapperFilter}
import org.nlp4l.core.analysis.Analyzer
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.util.Try
import scalax.file.Path

class DocSetSuite extends FunSuite with BeforeAndAfterAll {

  val indexDir = "/tmp/testindex_docsetsuite"
  val contentAnalyzer = Analyzer(new StandardAnalyzer)
  val fieldTypes = Map("title" -> FieldType(null, true, true), "content" -> FieldType(contentAnalyzer, true, true, termVectors = true, termPositions = true, termOffsets = true))
  val schema = Schema(Analyzer(new KeywordAnalyzer), fieldTypes)
  val docs = List(
    Document(Set(Field("title", "London Bridge"), Field("content", "London Bridge is falling down, Falling down, Falling down. London Bridge is falling down, My fair lady."))),
    Document(Set(Field("title", "London Bridge"), Field("content", "Take a key and lock her up, Lock her up, Lock her up. Take a key and lock her up, My fair lady. "))),
    Document(Set(Field("title", "London Bridge"), Field("content", "How will we build it up, Build it up, Build it up?　How will we build it up, My fair lady?"))),
    Document(Set(Field("title", "London Bridge"), Field("content", "Build it up with silver and gold, Silver and gold, Silver and gold.　Build it up with silver and gold, My fair lady."))),
    Document(Set(Field("title", "London Bridge"), Field("content", "Gold and silver I have none, I have none, I have none. Gold and silver I have none,　My fair lady."))),
    Document(Set(Field("title", "London Bridge"), Field("content", "Build it up with needles and pins,　Needles and pins, Needles and pins. Build it up with needles and pins, My fair lady."))),
    Document(Set(Field("title", "London Bridge"), Field("content", "Pins and needles bend and break, Bend and break,　Bend and break.　Pins and needles bend and break,　My fair lady."))),
    Document(Set(Field("title", "London Bridge"), Field("content", "Build it up with wood and clay, Wood and clay, Wood and clay. Build it up with wood and clay, My fair lady."))),
    Document(Set(Field("title", "London Bridge"), Field("content", "Wood and clay will wash away,　Wash away, Wash away.　Wood and clay will wash away, My fair lady."))),
    Document(Set(Field("title", "London Bridge"), Field("content", "Build it up with stone so strong, Stone so strong, Stone so strong. Build it up with stone so strong, My fair lady."))),
    Document(Set(Field("title", "London Bridge"), Field("content", "Stone so strong will last so long, Last so long, Last so long. Stone so strong will last so long, My fair lady.")))
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
  
  test("A IntDocSet holds document ids") {
    val reader = RawReader(indexDir)
    val filter = new QueryWrapperFilter(new TermQuery(new Term("content", "lady")))
    val docIdItr = filter.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    
    val docSet = new IntDocSet(docIdItr)
    assert(docSet.size == 11)
  }

  test("A BitDocSet holds document ids") {
    val reader = RawReader(indexDir)
    val filter = new QueryWrapperFilter(new TermQuery(new Term("content", "lady")))
    val docIdItr = filter.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()

    val docSet = new BitDocSet(docIdItr)
    assert(docSet.size == 11)
  }

  test("IntDocSet should support intersection") {
    val reader = RawReader(indexDir)
    
    val filter1 = new QueryWrapperFilter(new TermQuery(new Term("content", "build")))
    val docIdItr1 = filter1.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet1 = new IntDocSet(docIdItr1)
    
    val filter2 = new QueryWrapperFilter(new TermQuery(new Term("content", "gold")))
    val docIdItr2 = filter2.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet2 = new IntDocSet(docIdItr2)
    
    assertResult(Set(2,3,5,7,9))(docSet1.toSet)
    assertResult(Set(3,4))(docSet2.toSet)
    assertResult(Set(3))((docSet1 & docSet2).toSet)
  }

  test("BitDocSet should support intersection") {
    val reader = RawReader(indexDir)

    val filter1 = new QueryWrapperFilter(new TermQuery(new Term("content", "build")))
    val docIdItr1 = filter1.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet1 = new BitDocSet(docIdItr1)

    val filter2 = new QueryWrapperFilter(new TermQuery(new Term("content", "gold")))
    val docIdItr2 = filter2.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet2 = new BitDocSet(docIdItr2)

    assertResult(Set(2,3,5,7,9))(docSet1.toSet)
    assertResult(Set(3,4))(docSet2.toSet)
    assertResult(Set(3))((docSet1 & docSet2).toSet)
  }

  test("IntDocSet should support union") {
    val reader = RawReader(indexDir)

    val filter1 = new QueryWrapperFilter(new TermQuery(new Term("content", "build")))
    val docIdItr1 = filter1.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet1 = new IntDocSet(docIdItr1)

    val filter2 = new QueryWrapperFilter(new TermQuery(new Term("content", "gold")))
    val docIdItr2 = filter2.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet2 = new IntDocSet(docIdItr2)

    assertResult(Set(2,3,5,7,9))(docSet1.toSet)
    assertResult(Set(3,4))(docSet2.toSet)
    assertResult(Set(2,3,4,5,7,9))((docSet1 | docSet2).toSet)
  }

  test("BitDocSet should support union") {
    val reader = RawReader(indexDir)

    val filter1 = new QueryWrapperFilter(new TermQuery(new Term("content", "build")))
    val docIdItr1 = filter1.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet1 = new BitDocSet(docIdItr1)

    val filter2 = new QueryWrapperFilter(new TermQuery(new Term("content", "gold")))
    val docIdItr2 = filter2.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet2 = new BitDocSet(docIdItr2)

    assertResult(Set(2,3,5,7,9))(docSet1.toSet)
    assertResult(Set(3,4))(docSet2.toSet)
    assertResult(Set(2,3,4,5,7,9))((docSet1 | docSet2).toSet)
  }

  test("IntDocSet should support difference") {
    val reader = RawReader(indexDir)

    val filter1 = new QueryWrapperFilter(new TermQuery(new Term("content", "build")))
    val docIdItr1 = filter1.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet1 = new IntDocSet(docIdItr1)

    val filter2 = new QueryWrapperFilter(new TermQuery(new Term("content", "gold")))
    val docIdItr2 = filter2.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet2 = new IntDocSet(docIdItr2)

    assertResult(Set(2,3,5,7,9))(docSet1.toSet)
    assertResult(Set(3,4))(docSet2.toSet)
    assertResult(Set(2,5,7,9))((docSet1 &~ docSet2).toSet)
  }

  test("BitDocSet should support difference") {
    val reader = RawReader(indexDir)

    val filter1 = new QueryWrapperFilter(new TermQuery(new Term("content", "build")))
    val docIdItr1 = filter1.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet1 = new BitDocSet(docIdItr1)

    val filter2 = new QueryWrapperFilter(new TermQuery(new Term("content", "gold")))
    val docIdItr2 = filter2.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet2 = new BitDocSet(docIdItr2)

    assertResult(Set(2,3,5,7,9))(docSet1.toSet)
    assertResult(Set(3,4))(docSet2.toSet)
    assertResult(Set(2,5,7,9))((docSet1 &~ docSet2).toSet)
  }

  test("Empty IntDocSet should be returned when filter returns no docs") {
    val reader = RawReader(indexDir)
    val filter = new QueryWrapperFilter(new TermQuery(new Term("content", "abracadabra")))
    val docIdItr1 = filter.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet = new IntDocSet(docIdItr1)
    assert(docSet.isEmpty)
  }

  test("Empty BitDocSet should be returned when filter returns no docs") {
    val reader = RawReader(indexDir)
    val filter = new QueryWrapperFilter(new TermQuery(new Term("content", "abracadabra")))
    val docIdItr1 = filter.getDocIdSet(reader.ir.getContext, reader.liveDocs).iterator()
    val docSet = new BitDocSet(docIdItr1)
    assert(docSet.isEmpty)
  }

}
