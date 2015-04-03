package org.nlp4l.core

import java.nio.file.FileSystems

import org.apache.lucene.store.{Directory, FSDirectory, IOContext, IndexOutput, IndexInput}
import org.apache.lucene.util.IntsRefBuilder
import org.apache.lucene.util.fst._

object SimpleFST {
  def apply(generateUnknownWords: Boolean = false) = new SimpleFST(generateUnknownWords)
}

class SimpleFST(generateUnknownWords: Boolean) {
  val outputs = PositiveIntOutputs.getSingleton.asInstanceOf[Outputs[Long]]
  val builder: Builder[Long] = new Builder[Long](FST.INPUT_TYPE.BYTE4, outputs)
  val scratchInts = new IntsRefBuilder
  val scratchArc = new FST.Arc[Long]
  var fst: FST[Long] = null
  var fstReader: FST.BytesReader = null
  val MAX_LEN_UNKNOWN_WORD = 4
  val UNKNOWN_WORD: Long = -1

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
      if(fst.findTargetArc(codePoint, scratchArc, scratchArc, fstReader) == null){
        if(result.size > 0 || !generateUnknownWords) result
        else{
          unknownWords(str, pos, 1, result)  // result is empty
        }
      }
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

  private def unknownWords(str: String, pos: Int, index: Int, result: List[(Int, Long)]): Seq[(Int, Long)] = {
    if(str.length < pos + index || index > MAX_LEN_UNKNOWN_WORD) result
    else{
      unknownWords(str, pos, index + 1, result :+ (index, UNKNOWN_WORD))
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
