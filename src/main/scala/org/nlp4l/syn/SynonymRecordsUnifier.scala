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

object SynonymRecordsUnifier {

  def main(args: Array[String]): Unit = {
    if(args.length < 1) usage
    
    var synRecsList = List[SynonymRecords]()
    for(arg <- args){
      synRecsList = synRecsList :+ new SynonymRecords(arg, SynonymCommon.readAllRecords(arg))
    }
    
    outputUniqueSynonymRecords(synRecsList.head, synRecsList.tail)
  }
  
  def usage(): Unit = {
    println("Usage: org.nlp4l.syn.SynonymRecordsUnifier synonyms.txt [synonyms-2.txt synonyms-3.txt...]");
    println("\tsynonyms.txt\tsynonyms file to be checked")
    sys.exit
  }
  
  def outputUniqueSynonymRecords(src: SynonymRecords, destList: Seq[SynonymRecords]): Unit = {
    if(destList.isEmpty){
      outputCheckedFile(src.headerComments, src.uniqueRecords, src.outFile)
    }
    else{
      val result = checkAcross(src, destList, List())
      outputCheckedFile(src.headerComments, result._1.uniqueRecords, src.outFile)
      outputUniqueSynonymRecords(result._2.head, result._2.tail)
    }
  }
  
  def checkAcross(src: SynonymRecords, destList: Seq[SynonymRecords], checkedDest: Seq[SynonymRecords]):
    (SynonymRecords, Seq[SynonymRecords]) = {
    if(destList.isEmpty){
      (src, checkedDest)
    }
    else{
      val checkedLists = checkTwoRecordsList(src, destList.head)
      checkAcross(checkedLists._1, destList.tail, checkedDest :+ checkedLists._2)
    }
  }
  
  def checkTwoRecordsList(src: SynonymRecords, dest: SynonymRecords): (SynonymRecords, SynonymRecords) = {
    val result = checkTwoRecordsList(src.uniqueRecords, dest.uniqueRecords, List())
    (new SynonymRecords(src.synFile, src.outFile, src.headerComments, result._1),
      new SynonymRecords(dest.synFile, dest.outFile, dest.headerComments, result._2))
  }
  
  def checkTwoRecordsList(src: Seq[Seq[String]], dest: Seq[Seq[String]], outSrc: Seq[Seq[String]]):
    (Seq[Seq[String]], Seq[Seq[String]]) = {
    if(src.isEmpty){
      (outSrc, dest)
    }
    else{
      val result = checkRecord2List(src.head, dest, List())
      checkTwoRecordsList(src.tail, result._2, outSrc :+ result._1)
    }
  }
  
  def checkRecord2List(srcRecord: Seq[String], destList: Seq[Seq[String]], outDest: Seq[Seq[String]]):
    (Seq[String], Seq[Seq[String]]) = {
    if(destList.isEmpty){
      (srcRecord, outDest)
    }
    else{
      val unifiedSrcRecord = SynonymCommon.unifyRecordsIfNeeded(srcRecord, destList.head)
      if(unifiedSrcRecord == null){
        checkRecord2List(srcRecord, destList.tail, outDest :+ destList.head)
      }
      else{
        checkRecord2List(unifiedSrcRecord, destList.tail, outDest)
      }
    }
  }
  
  def outputCheckedFile(headerComments: Seq[String], records: Seq[Seq[String]], outFile: String): Unit ={
    if(records.nonEmpty){
      val pw = new PrintWriter(outFile)
      try{
        // write header comment lines
        for(headerComment <- headerComments){
          pw.println(headerComment)
        }
        
        // write synonym lines
        for(record <- records){
          pw.println(record.mkString(","))
        }
      }
      finally{
        pw.close
      }
    }
  }
}
