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

  test("FST in memory random number test"){
    DATA.foreach{ k =>
      val result = fst.leftMostSubstring(k._1, 0)
      assert(result.size == 1)
      assert(result.head._1 == k._1.length)
      assert(result.head._2 == k._2)
    }
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
