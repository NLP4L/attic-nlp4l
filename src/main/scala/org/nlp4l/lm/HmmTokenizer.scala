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

package org.nlp4l.lm

object HmmTokenizer {
  def apply(model: HmmModel) = new HmmTokenizer(model)
}

class HmmTokenizer(model: HmmModel) {

  var nodesTableByEnd = scala.collection.mutable.Map.empty[Int, Node]
  val CLASS_BOS = -2
  val CLASS_EOS = -2
  val debug = true

  def tokens(str: String): Seq[Token] = {
    nodesTableByEnd.clear()
    val node = parseForward(str)
    backTrace(str, node.backLink)
  }

  def parseForward(str: String): Node = {
    val BOS = Node(CLASS_BOS, 0, -1, 0, 0)
    val EOS = Node(CLASS_EOS, 0, str.length, -1)
    addNodeToLattice(0, BOS)
    parseForward(str, 0, EOS)
  }

  def parseForward(str: String, pos: Int, EOS: Node): Node = {
    if(pos == str.length){
      val leftNode = nodesTableByEnd.getOrElse(pos, null)
      processLeftLink(leftNode, EOS)
      EOS
    }
    else{
      val wrds = model.fst.leftMostSubstring(str, pos)
      wrds.foreach{ wrd =>
        val epos = wrd._1
        val classes = model.conditionalClassesCost(wrd._2.toInt)
        debugPrintWord(str, pos, epos, classes)
        classes.foreach{ cls =>
          val node = Node(cls._1, cls._2, pos, epos)
          addNodeToLattice(epos, node)
          val leftNode = nodesTableByEnd.getOrElse(pos, null)
          processLeftLink(leftNode, node)
        }
      }
      parseForward(str, pos + 1, EOS)
    }
  }

  private def debugPrintWord(str: String, spos: Int, epos: Int, classes: List[(Int, Int)]): Unit = {
    if(debug){
      println("%d %d".format(spos, epos))
      println("%s %s".format(str.substring(spos, epos), classes.map(e => (model.className(e._1), e._2))))
    }
  }

  def addNodeToLattice(pos: Int, node: Node): Unit = {
    val topNode = nodesTableByEnd.getOrElse(pos, null)
    if(topNode != null){
      nodesTableByEnd -= pos
      node.nextSameEnd = topNode
    }
    nodesTableByEnd += (pos -> node)
  }

  private def processLeftLink(leftNode: Node, rightNode: Node): Unit = {
    if(leftNode != null){
      rightNode.replaceTotalCostIfSmaller(leftNode)
      processLeftLink(leftNode.nextSameEnd, rightNode)
    }
  }

  def backTrace(str: String, node: Node): Seq[Token] = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[Token]
    backTrace(str, node, buf)
  }

  def backTrace(str: String, node: Node, buf: scala.collection.mutable.ArrayBuffer[Token]): Seq[Token] = {
    if(node.cls == CLASS_BOS){
      buf.toList.reverse
    }
    else{
      val token = Token(str.substring(node.spos, node.epos), model.className(node.cls))
      buf += token
      backTrace(str, node.backLink, buf)
    }
  }

  case class Token(word: String, cls: String)

  case class Node(cls: Int, cost: Int, spos: Int, epos: Int, tcost: Int = Int.MaxValue){

    var backLink: Node = null
    var total: Int = tcost
    var nextSameEnd: Node = null

    def replaceTotalCostIfSmaller(leftNode: Node): Unit = {
      if(leftNode.total + cost < total){
        backLink = leftNode
        total = leftNode.total + cost
      }
    }
  }
}
