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

import java.io.PrintWriter
import scala.sys.SystemProperties
import java.io.File

trait SynonymTest {
  
  val TEMP_DIR = new SystemProperties().apply("java.io.tmpdir")
  
  def l(a: String): Seq[String] = {
    a.split(",")
  }
  
  def ll(a: String): Seq[Seq[String]] = {
    val result = scala.collection.mutable.ArrayBuffer[Seq[String]]()
    a.split("/").foreach{ s =>
      result.append(l(s))
    }
    return result
  }
  
  def assertL(expected: Seq[String], actual: Seq[String]): Unit = {
    if(expected.isEmpty){
      assert(actual.isEmpty)
    }
    else{
      assert(actual.nonEmpty)
      assert(expected.head == actual.head, "expected \"%s\", but was \"%s\"".format(expected.head, actual.head))
      assertL(expected.tail, actual.tail)
    }
  }
  
  def assertLL(expected: Seq[Seq[String]], actual: Seq[Seq[String]]): Unit = {
    if(expected.isEmpty){
      assert(actual.isEmpty)
    }
    else{
      assert(actual.nonEmpty)
      assertL(expected.head, actual.head)
      assertLL(expected.tail, actual.tail)
    }
  }
  
  def tempFile(file: String): String ={
    "%s/%s".format(TEMP_DIR, file)
  }
  
  def createSynonymFile(file: String, content: String): Unit = {
    val lines = ll(content)
    val out = new PrintWriter(file)
    
    try{
      for(line <- lines){
        out.println(line.mkString(","))
      }
    }
    finally{
      out.close
    }
  }
  
  def assertSynonymFile(llExpected: String, fileActual: String): Unit = {
    val cont = SynonymCommon.readAllRecords(fileActual)
    assertLL(ll(llExpected), cont._2)
  }
  
  def rmFile(file: String): Unit ={
    val f = new File(file)
    f.delete()
  }
}
