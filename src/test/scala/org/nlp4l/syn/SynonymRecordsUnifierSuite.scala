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

class SynonymRecordsUnifierSuite extends FunSuite with SynonymTest {

  // ----------------------------------------------------
  // checkRecord2List
  test("checkRecord2List - unique record and list"){
    val result = SynonymRecordsUnifier.checkRecord2List(l("A,B,C"), ll("D,E/F,G,H/I,J"), List())
    assertL(l("A,B,C"), result._1)
    assertLL(ll("D,E/F,G,H/I,J"), result._2)
  }

  test("checkRecord2List - partially overlap"){
    val result = SynonymRecordsUnifier.checkRecord2List(l("A,B,C"), ll("D,E/F,B,H/I,J"), List())
    println("*** %s".format(result._1))
    assertL(l("A,B,C,F,H"), result._1)
    assertLL(ll("D,E/I,J"), result._2)
  }

  test("checkRecord2List - partially overlap (2)"){
    val result = SynonymRecordsUnifier.checkRecord2List(l("A,B,C"), ll("D,E/C,B,A/I,J"), List())
    assertL(l("A,B,C"), result._1)
    assertLL(ll("D,E/I,J"), result._2)
  }

  test("checkRecord2List - partially whole"){
    val result = SynonymRecordsUnifier.checkRecord2List(l("A,B,C"), ll("D,C/F,B,H/A,J"), List())
    assertL(l("A,B,C,D,F,H,J"), result._1)
    assert(result._2.isEmpty)
  }

  // ----------------------------------------------------
  // checkTwoRecordsList
  test("checkTwoRecordsList - 2 lists are unique each other"){
    val result = SynonymRecordsUnifier.checkTwoRecordsList(ll("A,B/C,D/E,F"), ll("G,H/I,J/K,L/M,N"), List())
    assertLL(ll("A,B/C,D/E,F"), result._1)
    assertLL(ll("G,H/I,J/K,L/M,N"), result._2)
  }

  test("checkTwoRecordsList - 1 record overlap"){
    val result = SynonymRecordsUnifier.checkTwoRecordsList(ll("A,B/C,D/E,F"), ll("G,H/I,J/C,D/M,N"), List())
    assertLL(ll("A,B/C,D/E,F"), result._1)
    assertLL(ll("G,H/I,J/M,N"), result._2)
  }

  test("checkTwoRecordsList - 2 records overlap"){
    val result = SynonymRecordsUnifier.checkTwoRecordsList(ll("A,B/C,D/E,F"), ll("G,H/I,J/C,D/B,M"), List())
    assertLL(ll("A,B,M/C,D/E,F"), result._1)
    assertLL(ll("G,H/I,J"), result._2)
  }
  
  test("main - 1 file"){
    val FILE = tempFile("SynonymRecordsUnifierSuiteMain1.txt")
    val FILE_CHECKED = "%s_checked".format(FILE)
    createSynonymFile(FILE, "A,B/C,D/E,F/B,G,H")
    SynonymRecordsUnifier.main(Array(FILE))
    assertSynonymFile("A,B,G,H/C,D/E,F", FILE_CHECKED)
    
    // clean up
    rmFile(FILE_CHECKED)
    rmFile(FILE)
  }
  
  test("main - 2 files (no overlaps)"){
    // file 1
    val FILE1 = tempFile("SynonymRecordsUnifierSuiteMain2-1.txt")
    val FILE1_CHECKED = "%s_checked".format(FILE1)
    createSynonymFile(FILE1, "A,B/C,D/E,F/C,G,H")

    // file 2
    val FILE2 = tempFile("SynonymRecordsUnifierSuiteMain2-2.txt")
    val FILE2_CHECKED = "%s_checked".format(FILE2)
    createSynonymFile(FILE2, "I,J/K,L,M,N/O,P")

    SynonymRecordsUnifier.main(Array(FILE1, FILE2))
    assertSynonymFile("A,B/C,D,G,H/E,F", FILE1_CHECKED)
    assertSynonymFile("I,J/K,L,M,N/O,P", FILE2_CHECKED)
    
    // clean up
    rmFile(FILE2_CHECKED)
    rmFile(FILE1_CHECKED)
    rmFile(FILE2)
    rmFile(FILE1)
  }
  
  test("main - 2 files (an overlap)"){
    // file 1
    val FILE1 = tempFile("SynonymRecordsUnifierSuiteMain2-1.txt")
    val FILE1_CHECKED = "%s_checked".format(FILE1)
    createSynonymFile(FILE1, "A,B/C,D/E,F/C,G,H")

    // file 2
    val FILE2 = tempFile("SynonymRecordsUnifierSuiteMain2-2.txt")
    val FILE2_CHECKED = "%s_checked".format(FILE2)
    createSynonymFile(FILE2, "I,J/K,L,M,G/O,P")

    SynonymRecordsUnifier.main(Array(FILE1, FILE2))
    assertSynonymFile("A,B/C,D,G,H,K,L,M/E,F", FILE1_CHECKED)
    assertSynonymFile("I,J/O,P", FILE2_CHECKED)
    
    // clean up
    rmFile(FILE2_CHECKED)
    rmFile(FILE1_CHECKED)
    rmFile(FILE2)
    rmFile(FILE1)
  }
  
  test("main - 3 files (simple overlaps)"){
    // file 1
    val FILE1 = tempFile("SynonymRecordsUnifierSuiteMain2-1.txt")
    val FILE1_CHECKED = "%s_checked".format(FILE1)
    createSynonymFile(FILE1, "A,B/C,D/E,F/C,G,H")

    // file 2
    val FILE2 = tempFile("SynonymRecordsUnifierSuiteMain2-2.txt")
    val FILE2_CHECKED = "%s_checked".format(FILE2)
    createSynonymFile(FILE2, "I,J/K,L,M,G/O,P")

    // file 3
    val FILE3 = tempFile("SynonymRecordsUnifierSuiteMain2-3.txt")
    val FILE3_CHECKED = "%s_checked".format(FILE3)
    createSynonymFile(FILE3, "Q,R,B/S,T,U")

    SynonymRecordsUnifier.main(Array(FILE1, FILE2, FILE3))
    assertSynonymFile("A,B,Q,R/C,D,G,H,K,L,M/E,F", FILE1_CHECKED)
    assertSynonymFile("I,J/O,P", FILE2_CHECKED)
    assertSynonymFile("S,T,U", FILE3_CHECKED)
    
    // clean up
    rmFile(FILE3_CHECKED)
    rmFile(FILE2_CHECKED)
    rmFile(FILE1_CHECKED)
    rmFile(FILE3)
    rmFile(FILE2)
    rmFile(FILE1)
  }
  
  test("main - 3 files (nested overlaps)"){
    // file 1
    val FILE1 = tempFile("SynonymRecordsUnifierSuiteMain2-1.txt")
    val FILE1_CHECKED = "%s_checked".format(FILE1)
    createSynonymFile(FILE1, "A,B/C,D/E,F/C,G,H")

    // file 2
    val FILE2 = tempFile("SynonymRecordsUnifierSuiteMain2-2.txt")
    val FILE2_CHECKED = "%s_checked".format(FILE2)
    createSynonymFile(FILE2, "I,J/K,L,M,G/O,P")

    // file 3
    val FILE3 = tempFile("SynonymRecordsUnifierSuiteMain2-3.txt")
    val FILE3_CHECKED = "%s_checked".format(FILE3)
    createSynonymFile(FILE3, "Q,R,I/S,T,U")

    SynonymRecordsUnifier.main(Array(FILE1, FILE2, FILE3))
    assertSynonymFile("A,B/C,D,G,H,K,L,M/E,F", FILE1_CHECKED)
    assertSynonymFile("I,J,Q,R/O,P", FILE2_CHECKED)
    assertSynonymFile("S,T,U", FILE3_CHECKED)
    
    // clean up
    rmFile(FILE3_CHECKED)
    rmFile(FILE2_CHECKED)
    rmFile(FILE1_CHECKED)
    rmFile(FILE3)
    rmFile(FILE2)
    rmFile(FILE1)
  }
}
