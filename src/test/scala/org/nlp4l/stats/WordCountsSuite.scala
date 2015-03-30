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

package org.nlp4l.stats

import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.Term
import org.apache.lucene.search._
import org.nlp4l.core._
import org.nlp4l.core.analysis.Analyzer
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FunSuite}

import scala.util.Try
import scalax.file.Path

class WordCountsSuite extends FunSuite with BeforeAndAfterAll {
  val indexDir = "/tmp/testindex_wordcountsuite"
  val contentAnalyzer = Analyzer(new StandardAnalyzer)
  val fieldTypes = Map("title" -> FieldType(null, true, true),
    "content1" -> FieldType(contentAnalyzer, true, true, termVectors = true),
    "content2" -> FieldType(contentAnalyzer, true, true, termVectors = false))
  val schema = Schema(Analyzer(new KeywordAnalyzer), fieldTypes)
  val docs = List(
    Document(Set(
      Field("title", "London Bridge A"), 
      Field("content1", "London Bridge is falling down, Falling down, Falling down. London Bridge is falling down, My fair lady."),
      Field("content2", "Take a key and lock her up, Lock her up, Lock her up. Take a key and lock her up, My fair lady. "))),
    Document(Set(
      Field("title", "London Bridge B"),
      Field("content1", "How will we build it up, Build it up, Build it up?　How will we build it up, My fair lady?"),
      Field("content2", "Build it up with silver and gold, Silver and gold, Silver and gold.　Build it up with silver and gold, My fair lady."))),
    Document(Set(
      Field("title", "London Bridge A"), 
      Field("content1", "Gold and silver I have none, I have none, I have none. Gold and silver I have none,　My fair lady."),
      Field("content2", "Build it up with needles and pins,　Needles and pins, Needles and pins. Build it up with needles and pins, My fair lady."))),
    Document(Set(
      Field("title", "London Bridge B"),
      Field("content1", "Pins and needles bend and break, Bend and break,　Bend and break.　Pins and needles bend and break,　My fair lady."),
      Field("content2", "Build it up with wood and clay, Wood and clay, Wood and clay. Build it up with wood and clay, My fair lady."))),
    Document(Set(
      Field("title", "London Bridge A"), 
      Field("content1", "Wood and clay will wash away,　Wash away, Wash away.　Wood and clay will wash away, My fair lady."),
      Field("content2", "Build it up with stone so strong, Stone so strong, Stone so strong. Build it up with stone so strong, My fair lady.")))
  )

  override def beforeAll {
    deleteIndexDir()

    val iw1 = IWriter(indexDir, schema)
    docs.foreach{ iw1.write(_) }
    iw1.close()
  }

  override def afterAll {
    deleteIndexDir()
  }

  private def deleteIndexDir(): Unit = {
    val path = Path.fromString(indexDir)
    Try(path.deleteRecursively(continueOnFailure = false))
  }

  test("counts all word frequencies in all documents") {
    val reader = IReader(indexDir, schema)
    val counts = WordCounts.count(reader, "content1", Set.empty[String], Set.empty[Int])
    assert(counts.size > 0)
    assertResult(5)(counts.getOrElse("lady", 0))
    assertResult(2)(counts.getOrElse("wood", 0))
    assertResult(4)(counts.getOrElse("up", 0))
  }

  test("counts all word frequencies in one document (with term vectors)") {
    val reader = IReader(indexDir, schema)
    val counts = WordCounts.count(reader, "content1", Set.empty[String], Set(0))
    assert(counts.size > 0)
    assertResult(1)(counts.getOrElse("lady", 0))
    assertResult(2)(counts.getOrElse("bridge", 0))
  }

  test("empty map returned if the field not exists") {
    val reader = IReader(indexDir, schema)
    val counts = WordCounts.count(reader, "unknown", Set.empty[String], Set.empty[Int])
    assert(counts.isEmpty)
  }

  test("counts word frequencies for top N words") {
    val reader = IReader(indexDir, schema)
    val counts = WordCounts.count(reader, "content1", Set.empty[String], Set.empty[Int], maxWords = 10)
    assertResult(10)(counts.size)
    assertResult(5)(counts.getOrElse("fair", 0))
    assertResult(5)(counts.getOrElse("lady", 0))
    assertResult(5)(counts.getOrElse("my", 0))
  }
  
  test("counts all word frequencies in specified documents set (with term vectors)") {
    val reader = IReader(indexDir, schema)
    val docset = reader.subset(TermFilter("title", "London Bridge A"))
    val counts = WordCounts.count(reader, "content1", Set.empty[String], docset)
    assert(counts.size > 0)
    assertResult(3)(counts.getOrElse("lady", 0))
    assertResult(2)(counts.getOrElse("wood", 0))
    assertResult(0)(counts.getOrElse("up", 0))
  }

  test("counts all word frequencies in specified documents set (without term vectors)") {
    val reader = IReader(indexDir, schema)
    val docset = reader.subset(TermFilter("title", "London Bridge A"))
    val counts = WordCounts.count(reader, "content2", Set.empty[String], docset)
    assert(counts.size > 0)
    assertResult(3)(counts.getOrElse("lady", 0))
    assertResult(0)(counts.getOrElse("wood", 0))
    assertResult(8)(counts.getOrElse("up", 0))
    
    // do same stuff with RawReader and Analyzer
    val reader2 = RawReader(indexDir)
    val counts2 = WordCounts.count(reader2, "content2", Set.empty[String], docset, -1, Analyzer(new StandardAnalyzer()))
    assert(counts2.size > 0)
    assertResult(3)(counts2.getOrElse("lady", 0))
    assertResult(0)(counts2.getOrElse("wood", 0))
    assertResult(8)(counts2.getOrElse("up", 0))
  }

  test("counts all word frequencies for specified words (with term vectors)") {
    val reader = IReader(indexDir, schema)
    val terms = Set("london", "gold", "build")
    val counts = WordCounts.count(reader, "content1", terms, Set.empty[Int])
    assertResult(3)(counts.size)
    assertResult(2)(counts.getOrElse("london", 0))
    assertResult(2)(counts.getOrElse("gold", 0))
    assertResult(4)(counts.getOrElse("build", 0))
  }

  test("total counts") {
    val reader = IReader(indexDir, schema)

    val counts1 = WordCounts.totalCount(reader, "content1", Set.empty[Int])
    assertResult(79)(counts1)

    val counts2 = WordCounts.totalCount(reader, "content2", Set.empty[Int])
    assertResult(83)(counts2)
  }

  test("unique word count after the specific prefix") {
    val reader = IReader(indexDir, schema)

    val counts = WordCounts.countPrefix(reader, "content2", "st")
    assertResult(2)(counts)
  }

}
