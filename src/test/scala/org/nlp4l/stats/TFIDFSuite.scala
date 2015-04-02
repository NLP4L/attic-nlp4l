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
import org.nlp4l.core._
import org.nlp4l.core.analysis.Analyzer
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.util.Try
import scalax.file.Path

class TFIDFSuite extends FunSuite with BeforeAndAfterAll {

  val indexDir = "/tmp/testindex_tfidfsuite"
  val contentAnalyzer = Analyzer(new StandardAnalyzer)
  val fieldTypes = Map("title" -> FieldType(null, true, true), "content" -> FieldType(contentAnalyzer, true, true, termVectors = true))
  val schema = Schema(Analyzer(new KeywordAnalyzer), fieldTypes)
  val docs = List(
    Document(Set(
      Field("title", "London Bridge"),
      Field("content", "London Bridge is falling down, Falling down, Falling down. London Bridge is falling down, My fair lady."))),
    Document(Set(
      Field("title", "London Bridge"),
      Field("content", "How will we build it up, Build it up, Build it up?　How will we build it up, My fair lady?"))),
    Document(Set(
      Field("title", "London Bridge"),
      Field("content", "Gold and silver I have none, I have none, I have none. Gold and silver I have none,　My fair lady."))),
    Document(Set(
      Field("title", "London Bridge"),
      Field("content", "Pins and needles bend and break, Bend and break,　Bend and break.　Pins and needles bend and break,　My fair lady."))),
    Document(Set(
      Field("title", "London Bridge"),
      Field("content", "Wood and clay will wash away,　Wash away, Wash away.　Wood and clay will wash away, My fair lady.")))
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

  test("generate a TF term vector from a document") {
    val reader = IReader(indexDir, schema)
    val (words, vector) = TFIDF.tfVector(reader, "content", 0)
    assert(words.size == vector.size)
    assertResult(Vector("bridge", "down", "fair", "falling", "lady", "london", "my"))(words)
    assertResult(Vector(2,4,1,4,1,2,1))(vector)
  }

  test("generate a TF term vector from a document with specified features") {
    val reader = IReader(indexDir, schema)
    val features = Set("bridge", "london", "lady", "gold")
    val (words, vector) = TFIDF.tfVector(reader, "content", 0, features)
    assertResult(Vector("bridge", "gold","lady", "london"))(words)
    assertResult(Vector(2,0,1,2))(vector)
  }

  test("generate TF term vectors from documents") {
    val reader = IReader(indexDir, schema)
    val (words, vectors) = TFIDF.tfVectors(reader, "content", List(0,1,2))
    assert(vectors.forall(_.length == words.length))
    assertResult(Vector("bridge", "build", "down", "fair", "falling", "gold", "have", "how", "i", "lady", "london", "my", "none", "silver", "up", "we"))(words)
    assertResult(Vector(2,0,4,1,4,0,0,0,0,1,2,1,0,0,0,0))(vectors(0))
    assertResult(Vector(0,4,0,1,0,0,0,2,0,1,0,1,0,0,4,2))(vectors(1))
    assertResult(Vector(0,0,0,1,0,2,4,0,4,1,0,1,4,2,0,0))(vectors(2))
  }

  test("generate TF term vectors from documents with specified features") {
    val reader = IReader(indexDir, schema)
    val features = Set("down", "go", "bridge", "silver", "make", "falling", "lady")
    val (words, vectors) = TFIDF.tfVectors(reader, "content", List(0,1,2), features)
    assertResult(Vector("bridge", "down", "falling", "go", "lady", "make", "silver"))(words)
    assertResult(Vector(2,4,4,0,1,0,0))(vectors(0))
    assertResult(Vector(0,0,0,0,1,0,0))(vectors(1))
    assertResult(Vector(0,0,0,0,1,0,2))(vectors(2))
  }

  test("generate a TF/IDF term vector from a document") {
    val reader = IReader(indexDir, schema)
    val N = reader.numDocs
    val dfMap = WordCounts.countDF(reader, "content", Set.empty)
    val (words, vector) = TFIDF.tfIdfVector(reader, "content", 0)
    assert(words.size == vector.size)
    assertResult(Vector("bridge", "down", "fair", "falling", "lady", "london", "my"))(words)
    assertResult(
      Vector(
        2 * math.log(N/dfMap("bridge").toDouble),
        4 * math.log(N/dfMap("down").toDouble),
        1 * math.log(N/dfMap("fair").toDouble),
        4 * math.log(N/dfMap("falling").toDouble),
        1 * math.log(N/dfMap("lady").toDouble),
        2 * math.log(N/dfMap("london").toDouble),
        1 * math.log(N/dfMap("my"))))(vector)
  }

  test("generate a TF/IDF term vector from a document with specified features") {
    val reader = IReader(indexDir, schema)
    val N = reader.numDocs
    val dfMap = WordCounts.countDF(reader, "content", Set.empty)
    val features = Set("bridge", "london", "lady", "gold")
    val (words, vector) = TFIDF.tfIdfVector(reader, "content", 0, features)
    assertResult(Vector("bridge", "gold","lady", "london"))(words)
    assertResult(
      Vector(
        2 * math.log(N/dfMap("bridge").toDouble),
        0.0,
        1 * math.log(N/dfMap("lady").toDouble),
        2 * math.log(N/dfMap("london").toDouble)))(vector)
  }

  test("generate TF/IDF term vectors from documents") {
    val reader = IReader(indexDir, schema)
    val N = reader.numDocs
    val dfMap = WordCounts.countDF(reader, "content", Set.empty)
    val (words, vectors) = TFIDF.tfIdfVectors(reader, "content", List(0,1,2))
    assert(vectors.forall(_.length == words.length))
    assertResult(Vector("bridge", "build", "down", "fair", "falling", "gold", "have", "how", "i", "lady", "london", "my", "none", "silver", "up", "we"))(words)
    assertResult(
      Vector(
        2  * math.log(N/dfMap("bridge").toDouble),
        0.0,
        4 * math.log(N/dfMap("down").toDouble),
        1 * math.log(N/dfMap("fair").toDouble),
        4 * math.log(N/dfMap("falling").toDouble),
        0.0,
        0.0,
        0.0,
        0.0,
        1 * math.log(N/dfMap("lady").toDouble),
        2 * math.log(N/dfMap("london").toDouble),
        1 * math.log(N/dfMap("my").toDouble),
        0.0,
        0.0,
        0.0,
        0.0))(vectors(0))
    assertResult(
      Vector(
        0.0,
        4 * math.log(N/dfMap("build").toDouble),
        0.0,
        1 * math.log(N/dfMap("fair").toDouble),
        0.0,
        0.0,
        0.0,
        2 * math.log(N/dfMap("how").toDouble),
        0.0,
        1 * math.log(N/dfMap("lady").toDouble),
        0.0,
        1 * math.log(N/dfMap("my").toDouble),
        0.0,
        0.0,
        4 * math.log(N/dfMap("up").toDouble),
        2 * math.log(N/dfMap("we").toDouble)))(vectors(1))
    assertResult(
      Vector(
        0.0,
        0.0,
        0.0,
        1 * math.log(N/dfMap("fair")),
        0.0,
        2 * math.log(N/dfMap("gold")),
        4 * math.log(N/dfMap("have")),
        0.0,
        4 * math.log(N/dfMap("i")),
        1 * math.log(N/dfMap("lady")),
        0.0,
        1 * math.log(N/dfMap("my")),
        4 * math.log(N/dfMap("none")),
        2 * math.log(N/dfMap("silver")),
        0.0,
        0.0))(vectors(2))
  }

  test("generate TF/IDF term vectors from documents with specified features") {
    val reader = IReader(indexDir, schema)
    val N = reader.numDocs
    val dfMap = WordCounts.countDF(reader, "content", Set.empty)
    val features = Set("down", "go", "bridge", "silver", "make", "falling", "lady")
    val (words, vectors) = TFIDF.tfIdfVectors(reader, "content", List(0,1,2), features)
    assertResult(Vector("bridge", "down", "falling", "go", "lady", "make", "silver"))(words)
    assertResult(
      Vector(
        2 * math.log(N/dfMap("bridge").toDouble),
        4 * math.log(N/dfMap("down").toDouble),
        4 * math.log(N/dfMap("falling").toDouble),
        0.0,
        1 * math.log(N/dfMap("lady").toDouble),
        0.0,
        0.0))(vectors(0))
    assertResult(
      Vector(
        0.0,
        0.0,
        0.0,
        0.0,
        1 * math.log(N/dfMap("lady").toDouble),
        0.0,
        0.0))(vectors(1))
    assertResult(
      Vector(
        0.0,
        0.0,
        0.0,
        0.0,
        1 * math.log(N/dfMap("lady").toDouble),
        0.0,
        2 * math.log(N/dfMap("silver").toDouble)))(vectors(2))
  }

}
