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

package org.nlp4l.lm

import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.util.Try
import scalax.file.Path

class HmmModelSuite extends FunSuite with BeforeAndAfterAll {

  val index = "/tmp/testhmmmodel"

  val CORPUS = Array(
    "a/x b/x c/y d/z",
    "b/y a/x c/z d/z",
    "c/z e/x b/y",
    "a/y c/y b/x d/x e/z a/x"
  )

  private def deleteDir(dir: String): Unit = {
    val path = Path.fromString(dir)
    Try(path.deleteRecursively(continueOnFailure = false))
  }

  // create simple HmmModel
  override def beforeAll: Unit = {
    val indexer = HmmModelIndexer(index)

    CORPUS.map{ s: String =>
      val wcs: Array[String] = s.split(" ")
      val doc = wcs.map { wc =>
        val wc1 = wc.split("/")
        Pair(wc1(0), wc1(1))
      }
      indexer.addDocument(doc)
    }

    indexer.close()
  }

  override def afterAll: Unit = {
    deleteDir(index)
  }

  test("check initial cost"){
    val model = HmmModel(index)

    assertResult(model.cost(1.0/4.0))(model.costInitialState(model.classNamesDic("x")))
    assertResult(model.cost(2.0/4.0))(model.costInitialState(model.classNamesDic("y")))
    assertResult(model.cost(1.0/4.0))(model.costInitialState(model.classNamesDic("z")))
  }

  test("test emission cost"){
    val model = HmmModel(index)

    val a_ccc = model.conditionalClassesCost(model.wordDic("a")).toMap
    assertResult(2)(a_ccc.size)
    assertResult(model.cost(3.0/7.0))(a_ccc.getOrElse(model.classNamesDic("x"), Int.MaxValue))
    assertResult(model.cost(1.0/5.0))(a_ccc.getOrElse(model.classNamesDic("y"), Int.MaxValue))

    val b_ccc = model.conditionalClassesCost(model.wordDic("b")).toMap
    assertResult(2)(b_ccc.size)
    assertResult(model.cost(2.0/7.0))(b_ccc.getOrElse(model.classNamesDic("x"), Int.MaxValue))
    assertResult(model.cost(2.0/5.0))(b_ccc.getOrElse(model.classNamesDic("y"), Int.MaxValue))

    val c_ccc = model.conditionalClassesCost(model.wordDic("c")).toMap
    assertResult(2)(c_ccc.size)
    assertResult(model.cost(2.0/5.0))(c_ccc.getOrElse(model.classNamesDic("y"), Int.MaxValue))
    assertResult(model.cost(2.0/5.0))(c_ccc.getOrElse(model.classNamesDic("z"), Int.MaxValue))

    val d_ccc = model.conditionalClassesCost(model.wordDic("d")).toMap
    assertResult(2)(d_ccc.size)
    assertResult(model.cost(1.0/7.0))(d_ccc.getOrElse(model.classNamesDic("x"), Int.MaxValue))
    assertResult(model.cost(2.0/5.0))(d_ccc.getOrElse(model.classNamesDic("z"), Int.MaxValue))

    val e_ccc = model.conditionalClassesCost(model.wordDic("e")).toMap
    assertResult(2)(e_ccc.size)
    assertResult(model.cost(1.0/7.0))(e_ccc.getOrElse(model.classNamesDic("x"), Int.MaxValue))
    assertResult(model.cost(1.0/5.0))(e_ccc.getOrElse(model.classNamesDic("z"), Int.MaxValue))
  }
}
