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

import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.util.Try
import scalax.file.Path

class SimpleFSTSuite extends FunSuite with BeforeAndAfterAll {

  val SOURCE_DATA = List(
    "king", "to", "been", "the", "that", "with", "seem", "have", "alive", "fact", "peculiarities", "caution"
  )
  val r = new scala.util.Random(java.lang.System.currentTimeMillis())
  val DATA: Seq[(String, Long)] = SOURCE_DATA.map(s => (s, r.nextLong.abs))
  val sorted = DATA.sortBy(_._1)

  val fst = SimpleFST()

  private def deleteDir(dir: String): Unit = {
    val path = Path.fromString(dir)
    Try(path.deleteRecursively(continueOnFailure = false))
  }

  override def beforeAll {
    sorted.foreach{ k =>
      fst.addEntry(k._1, k._2)
    }
    fst.finish()
  }

  test("FST in memory random number test for leftMostSubstring"){
    DATA.foreach{ k =>
      val result = fst.leftMostSubstring(k._1, 0)
      assert(result.size == 1)
      assert(result.head._1 == k._1.length)
      assert(result.head._2 == k._2)
    }
  }

  test("FST in memory random number test for exactMatch"){
    DATA.foreach{ k =>
      val result = fst.exactMatch(k._1)
      assert(result === k._2)
    }
    assert(fst.exactMatch("notexist") == -1)
  }

  test("FST save/load with random number test"){
    val dir = "/tmp/testfst"

    fst.save(dir)

    val fst2 = SimpleFST()
    fst2.load(dir)

    DATA.foreach{ k =>
      val result = fst2.leftMostSubstring(k._1, 0)
      assert(result.size == 1)
      assert(result.head._1 == k._1.length)
      assert(result.head._2 == k._2)
    }

    deleteDir(dir)
  }
}
