package org.nlp4l.core

import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexOptions
import org.nlp4l.core.analysis.Analyzer
import org.scalatest.FlatSpec

class SchemaAndDocumentSpec extends FlatSpec {

  "A Schema" must "has an default anazlyzer and field types" in {
    val defaultAnalyzer = new Analyzer(new KeywordAnalyzer())
    val analyzer = new Analyzer(new StandardAnalyzer())
    val fieldTypes = Map("f1" -> FieldType(analyzer, true, false), "f2" -> FieldType(null, false, true), "f3" -> FieldType(analyzer, true, false, true))
    val schema = Schema(defaultAnalyzer, fieldTypes)
    
    assert(schema.get("f1").get.isInstanceOf[FieldType])
    assert(schema.get("f2").get.isInstanceOf[FieldType])
    assert(schema.get("f3").get.isInstanceOf[FieldType])
    assert(schema.get("f4") == None)
    
    assert(schema.getAnalyzer("f1").get.delegate.getClass.getName == analyzer.delegate.getClass.getName)
    assert(schema.getAnalyzer("f2") == None)
    assert(schema.getAnalyzer("f4") == None)
    
    assert(schema.perFieldAnalyzer.isInstanceOf[PerFieldAnalyzerWrapper])
  }
  
  "A Document" must "has fields" in {
    val doc = Document(Set(Field("id", "PRODUCT_A"), Field("name", "smart tv"), Field("desc", List("aaa", "bbb", "ccc"))))
    assertResult(List("PRODUCT_A"))(doc.getValue("id").get)
    assertResult(List("smart tv"))(doc.getValue("name").get)
    assertResult(List("aaa", "bbb", "ccc"))(doc.getValue("desc").get)
    
    // unknown field
    assert(doc.get("unknown") == None)
    assert(doc.getValue("unknown") == None)
  }
  
  it should "be converted to Lucene Document with Schema" in {
    val defaultAnalyzer = new Analyzer(new KeywordAnalyzer())
    val analyzer = new Analyzer(new StandardAnalyzer())
    val fieldTypes = Map("id" -> FieldType(null, true, false), "name" -> FieldType(analyzer, true, true), "desc" -> FieldType(analyzer, true, false, true, true, true))
    val schema = Schema(defaultAnalyzer, fieldTypes)
    val doc = Document(Set(Field("id", "PRODUCT_A"), Field("name", "smart tv"), Field("desc", List("aaa", "bbb", "ccc")), Field("comment", "this is comment")))
    
    val ldoc = doc.luceneDocument(schema)
    
    val f1 = ldoc.getField("id")
    assertResult("id")(f1.name())
    assertResult("PRODUCT_A")(f1.stringValue())
    assert(f1.fieldType().indexOptions() == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)
    assert(!f1.fieldType().tokenized())
    assert(!f1.fieldType().stored())
    assert(!f1.fieldType().storeTermVectors())
    assert(!f1.fieldType().storeTermVectorPositions())
    assert(!f1.fieldType().storeTermVectorOffsets())
    
    val f2 = ldoc.getField("name")
    assertResult("name")(f2.name())
    assertResult("smart tv")(f2.stringValue())
    assert(f2.fieldType().indexOptions() == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)
    assert(f2.fieldType().tokenized())
    assert(f2.fieldType().stored())
    assert(!f2.fieldType().storeTermVectors())
    assert(!f2.fieldType().storeTermVectorPositions())
    assert(!f2.fieldType().storeTermVectorOffsets())
    
    assertResult(3)(ldoc.getFields("desc").length)
    assertResult("aaa")(ldoc.getFields("desc")(0).stringValue())
    assertResult("bbb")(ldoc.getFields("desc")(1).stringValue())
    assertResult("ccc")(ldoc.getFields("desc")(2).stringValue())
    
    val f3 = ldoc.getFields("desc")(0)
    assert(f3.fieldType().indexOptions() == IndexOptions.DOCS_AND_FREQS_AND_POSITIONS)
    assert(f3.fieldType().tokenized())
    assert(!f3.fieldType().stored())
    assert(f3.fieldType().storeTermVectors())
    assert(f3.fieldType().storeTermVectorPositions())
    assert(f3.fieldType().storeTermVectorOffsets())
    
    // no data indexed for not defined field in Schema
    assert(ldoc.getField("comment") == null)
  }
}
