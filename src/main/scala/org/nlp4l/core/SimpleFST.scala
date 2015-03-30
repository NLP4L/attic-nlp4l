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

import java.nio.file.FileSystems

import org.apache.lucene.store.{Directory, FSDirectory, IOContext, IndexOutput, IndexInput}
import org.apache.lucene.util.IntsRefBuilder
import org.apache.lucene.util.fst._

object SimpleFST {
  def apply() = new SimpleFST()
}

class SimpleFST() {
  val outputs = PositiveIntOutputs.getSingleton.asInstanceOf[Outputs[Long]]
  val builder: Builder[Long] = new Builder[Long](FST.INPUT_TYPE.BYTE4, outputs)
  val scratchInts = new IntsRefBuilder
  val scratchArc = new FST.Arc[Long]
  var fst: FST[Long] = null
  var fstReader: FST.BytesReader = null

  // add entries in alphabetical order
  def addEntry(text: String, value: Long): Unit = {
    builder.add(Util.toUTF32(text, scratchInts), value)
  }

  def finish() : Unit = {
    fst = builder.finish
    fstReader = fst.getBytesReader
  }

  def leftMostSubstring(str: String, pos: Int): Seq[(Int, Long)] = {
    val pendingOutput = outputs.getNoOutput
    fst.getFirstArc(scratchArc)
    leftMostSubstring(str, pos, 0, pendingOutput, List.empty[(Int, Long)])
  }

  private def leftMostSubstring(str: String, pos: Int, index: Int, pendingOutput: Long, result: List[(Int, Long)]): Seq[(Int, Long)] = {
    if(str.length <= pos + index) result
    else{
      val codePoint = str.codePointAt(pos + index)
      if(fst.findTargetArc(codePoint, scratchArc, scratchArc, fstReader) == null) result
      else{
        val nextIndex = index + Character.charCount(codePoint)
        val pendingOutput2 = fst.outputs.add(pendingOutput, scratchArc.output)
        if(scratchArc.isFinal()){
          val matchOutputs = fst.outputs.add(pendingOutput2, scratchArc.nextFinalOutput)
          leftMostSubstring(str, pos, nextIndex, matchOutputs, result :+ (pos + nextIndex, matchOutputs))
        }
        else{
          leftMostSubstring(str, pos, nextIndex, pendingOutput2, result)
        }
      }
    }
  }

  def save(dirStr: String, file: String = "fst.dic"): Unit = {
    val dir: Directory = FSDirectory.open(FileSystems.getDefault.getPath(dirStr))
    val out: IndexOutput = dir.createOutput(file, IOContext.DEFAULT)
    fst.save(out)
    out.close()
    dir.close()
  }

  def load(dirStr: String, file: String = "fst.dic"): Unit = {
    val dir: Directory = FSDirectory.open(FileSystems.getDefault.getPath(dirStr))
    val in: IndexInput = dir.openInput(file, IOContext.DEFAULT)
    fst = new FST[Long](in, outputs)
    fstReader = fst.getBytesReader
  }
}
