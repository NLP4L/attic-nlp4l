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

package org.nlp4l.syn

import org.scalatest.FunSuite

class SynonymCommonSuite extends FunSuite with SynonymTest {

  // ----------------------------------------------------
  // unifyRecordsIfNeeded
  test("unifyRecordsIfNeeded - two different lists"){
    val result = SynonymCommon.unifyRecordsIfNeeded(l("A,B,C"), l("D,E,F"))
    assert(result == null)
  }
  
  test("unifyRecordsIfNeeded - the fisrt list includes a part of second list"){
    val result = SynonymCommon.unifyRecordsIfNeeded(l("A,B,C"), l("D,B"))
    assertL(l("A,B,C,D"), result)
  }

  test("unifyRecordsIfNeeded - the fisrt list includes all member of second list"){
    val result = SynonymCommon.unifyRecordsIfNeeded(l("A,B,C"), l("C,A,B"))
    assertL(l("A,B,C"), result)
  }

  // ----------------------------------------------------
  // checkRecords
  test("checkRecords - the fisrt list doesn't match"){
    val result = SynonymCommon.checkRecords(l("A,B,C"), ll("D,E,F/G,H,I/J,K"), List(), List())
    assertL(l("A,B,C"), result._1.head)
    assertLL(ll("D,E,F/G,H,I/J,K"), result._2)
  }
  
  test("checkRecords - the fisrt list match one time"){
    val result = SynonymCommon.checkRecords(l("A,B,C"), ll("D,E,F/B,G/J,K"), List(), List())
    assertL(l("A,B,C,G"), result._1.head)
    assertLL(ll("D,E,F/J,K"), result._2)
  }
  
  test("checkRecords - the fisrt list match two times"){
    val result = SynonymCommon.checkRecords(l("A,B,C"), ll("D,E,F/B,G/J,K,G"), List(), List())
    assertL(l("A,B,C,G,J,K"), result._1.head)
    assertLL(ll("D,E,F"), result._2)
  }

  // ----------------------------------------------------
  // getUniqueRecords
  test("getUniqueRecords - no merge"){
    val result = SynonymCommon.getUniqueRecords(ll("A,B,C/D,E,F/G,H,I/J,K"), List())
    assertLL(ll("A,B,C/D,E,F/G,H,I/J,K"), result)
  }
  
  test("getUniqueRecords - one merge"){
    val result = SynonymCommon.getUniqueRecords(ll("A,B,C/D,E,F/G,H,I/F,J,K"), List())
    assertLL(ll("A,B,C/D,E,F,J,K/G,H,I"), result)
  }
  
  test("getUniqueRecords - two merges"){
    val result = SynonymCommon.getUniqueRecords(ll("A,B,C/D,E,F/G,C,I/F,J,K"), List())
    assertLL(ll("A,B,C,G,I/D,E,F,J,K"), result)
  }
}
