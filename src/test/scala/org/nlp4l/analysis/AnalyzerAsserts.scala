package org.nlp4l.core.analysis

import org.scalatest.FunSuite

trait AnalyzerAsserts extends FunSuite {
  def assertToken(token: Token, expected: Map[String, String]): Unit = {
    expected.foreach{ e =>
      val result = token.getOrElse(e._1, "*no value*")
      assert(result === e._2)
    }
  }
}
