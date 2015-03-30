package org.nlp4l.core

import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.nlp4l.core.analysis.Analyzer
import org.scalatest.FlatSpec

import scala.util.Try
import scalax.file.Path

class ReadWriteSpec extends FlatSpec {

  val indexDir = "/tmp/testindex_readwritespec"
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

  "IWriter and IReader" should "writes/reads schema-aware index" in {
    val defaultAnalyzer =Analyzer(new KeywordAnalyzer)
    val contentAnalyzer = Analyzer(new StandardAnalyzer)
    val fieldTypes = Map("id" -> FieldType(null, true, true), "title" -> FieldType(null, true, true), "content" -> FieldType(contentAnalyzer, true, true, termVectors = true, termPositions = true, termOffsets = true))
    val schema = Schema(defaultAnalyzer, fieldTypes)

    val iw = IWriter(indexDir, schema)
    docs.foreach{ iw.write(_) }
    iw.close()

    val ir = IReader(indexDir, schema)
    assert(ir.getAnalyzer("title") == None)
    assert(ir.getAnalyzer("content").get.delegate.getClass.getName equals contentAnalyzer.delegate.getClass.getName)
    
    val tv = ir.getTermVector(1, "content").get
    assert(tv.hasPositions)
    assert(tv.hasOffsets)

    deleteIndexDir()
  }

  private def deleteIndexDir(): Unit = {
    val path = Path.fromString(indexDir)
    Try(path.deleteRecursively(continueOnFailure = false))
  }

}
