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

import org.scalatest.FunSuite

class StatsSuite extends FunSuite {
  test("average of 1, 2, 3, ..., 10 should be 5.5"){
    assert(Stats.average(1.toLong to 10.toLong toList) == 5.5)
  }

  test("variance of 1, 2, 3, ..., 10 should be 8.25"){
    assert(Stats.variance(1.toLong to 10.toLong toList) == 8.25)
  }

  test("covariance of (1,2,3,4,5) and (5,2,3,4,1) should be -1.2"){
    assert(Stats.covariance(List(1, 2, 3, 4, 5).map(_.toLong), List(5, 2, 3, 4, 1).map(_.toLong)) === -1.2)
  }

  test("correlation coefficient of (1,2,3,4,5) and (5,2,3,4,1) should be -0.6"){
    assert(Stats.correlationCoefficient(List(1, 2, 3, 4, 5).map(_.toLong), List(5, 2, 3, 4, 1).map(_.toLong)) === -0.6)
  }

  // TODO: use Equality trait
  test("chi-square should be correct value when not using Yates' correction"){
    assert(Stats.chiSquare(160, List(167000, 279).map(_.toLong), 24, List(36000, 797).map(_.toLong), false) === 3.094676826950349)
    assert(Stats.chiSquare(802, List(166000 ,637).map(_.toLong), 73, List(36000 ,748).map(_.toLong), false) === 55.76575227173258)
  }

  // TODO: use Equality trait
  test("chi-square should be correct value when using Yates' correction"){
    assert(Stats.chiSquare(160, List(167000, 279).map(_.toLong), 24, List(36000, 797).map(_.toLong), true) === 2.7663581251024776)
    assert(Stats.chiSquare(802, List(166000 ,637).map(_.toLong), 73, List(36000 ,748).map(_.toLong), true) === 55.10955273562374)
  }
}
