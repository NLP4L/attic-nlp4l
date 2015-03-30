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
