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

package org.nlp4l.colloc

import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.util.Try
import scalax.file.Path

class CollocationalAnalysisModelSuite extends FunSuite with BeforeAndAfterAll {

  val index = "/tmp/testcollocanalmodel"

  val CORPUS = Array(
    "a4 b3 c2 d e1 f1 g1",
    "a3 b2 c1 d e2 f2 g2",
    "a2 b1 c2 d e1 f3 g3",
    "a1 b3 c1 d e2 f1 g4",
    "a4 b2 c2 d e1 f2 g1",
    "a3 b1 c1 d e2 f3 g2",
    "a2 b3 c2 d e1 f1 g3",
    "a1 b2 c1 d e2 f2 g4",
    "a4 b1 c2 d e1 f3 g1",
    "a3 b3 c1 d e2 f1 g2"
  )

  private def deleteDir(dir: String): Unit = {
    val path = Path.fromString(dir)
    Try(path.deleteRecursively(continueOnFailure = false))
  }

  // create simple CollocationalAnalysisModel
  override def beforeAll: Unit = {
    val indexer = CollocationalAnalysisModelIndexer(index, new WhitespaceTokenizerFactory(new java.util.HashMap[String, String]()))

    CORPUS.foreach(indexer.addDocument(_))

    indexer.close()
  }

  override def afterAll: Unit = {
    deleteDir(index)
  }

  test("check surrounding of the word d"){
    val model = CollocationalAnalysisModel(index)

    val result = model.collocationalWordsStats("d", 10)

    assertResult(6)(result.size)

    // check word1 field
    assertResult(2)(result(0).size)
    assertResult("e1")(result(0)(0)._1)
    assertResult(5)(result(0)(0)._2)
    assertResult("e2")(result(0)(1)._1)
    assertResult(5)(result(0)(1)._2)

    // check word2 field
    assertResult(3)(result(1).size)
    assertResult("f1")(result(1)(0)._1)
    assertResult(4)(result(1)(0)._2)
    assertResult("f2")(result(1)(1)._1)
    assertResult(3)(result(1)(1)._2)
    assertResult("f3")(result(1)(2)._1)
    assertResult(3)(result(1)(2)._2)

    // check word3 field
    assertResult(4)(result(2).size)
    assertResult("g1")(result(2)(0)._1)
    assertResult(3)(result(2)(0)._2)
    assertResult("g2")(result(2)(1)._1)
    assertResult(3)(result(2)(1)._2)
    assertResult("g3")(result(2)(2)._1)
    assertResult(2)(result(2)(2)._2)
    assertResult("g4")(result(2)(3)._1)
    assertResult(2)(result(2)(3)._2)

    // check word1r field
    assertResult(2)(result(3).size)
    assertResult("c1")(result(3)(0)._1)
    assertResult(5)(result(3)(0)._2)
    assertResult("c2")(result(3)(1)._1)
    assertResult(5)(result(3)(1)._2)

    // check word2r field
    assertResult(3)(result(4).size)
    assertResult("b3")(result(4)(0)._1)
    assertResult(4)(result(4)(0)._2)
    assertResult("b1")(result(4)(1)._1)
    assertResult(3)(result(4)(1)._2)
    assertResult("b2")(result(4)(2)._1)
    assertResult(3)(result(4)(2)._2)

    // check word3r field
    assertResult(4)(result(5).size)
    assertResult("a3")(result(5)(0)._1)
    assertResult(3)(result(5)(0)._2)
    assertResult("a4")(result(5)(1)._1)
    assertResult(3)(result(5)(1)._2)
    assertResult("a1")(result(5)(2)._1)
    assertResult(2)(result(5)(2)._2)
    assertResult("a2")(result(5)(3)._1)
    assertResult(2)(result(5)(3)._2)

    model.close()
  }
}
