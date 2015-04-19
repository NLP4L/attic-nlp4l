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

object HmmTagger {
  def apply(model: HmmModel) = new HmmTagger(model)
}

class HmmTagger(model: HmmModel) {

  var lattice: Array[Node] = null
  val CLASS_BOS = -2
  val CLASS_EOS = -2
  val debug = true

  def tokens(str: String): Seq[Token] = {
    val words = str.split("\\s+").toList
    lattice = new Array[Node](words.length + 1)
    val node = parseForward(words)
    backTrace(str, node.backLink)
  }

  def parseForward(words: List[String]): Node = {
    val BOS = Node("<s>", CLASS_BOS, 0, -1, 0)
    val EOS = Node("</s>", CLASS_EOS, 0, words.length, 0)
    addNodeToLattice(-1, BOS)
    parseForward(words, 0, EOS)
  }

  def parseForward(words: List[String], pos: Int, EOS: Node): Node = {
    if(pos == words.length){
      val leftNode = lattice(pos)
      processLeftLink(leftNode, EOS)
      EOS
    }
    else{
      val word = model.fst.exactMatch(words(pos))
      val classes = model.conditionalClassesCost(word.toInt)
      debugPrintWord(words(pos), pos, classes)
      classes.foreach{ cls =>
        val node = Node(words(pos), cls._1, cls._2, pos)
        addNodeToLattice(pos, node)
        val leftNode = lattice(pos)
        processLeftLink(leftNode, node)
      }
      parseForward(words, pos + 1, EOS)
    }
  }

  private def debugPrintWord(word: String, pos: Int, classes: List[(Int, Int)]): Unit = {
    if(debug){
      println("%d".format(pos))
      println("%s %s".format(word, classes.map(e => (model.className(e._1), e._2))))
    }
  }

  def addNodeToLattice(pos: Int, node: Node): Unit = {
    val idx = pos + 1
    val topNode = lattice(idx)
    if(topNode == null) lattice(idx) = node
    else{
      node.nextSameEnd = topNode
      lattice(idx) = node
    }
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
      val token = Token(node.word, model.className(node.cls))
      buf += token
      backTrace(str, node.backLink, buf)
    }
  }

  case class Token(word: String, cls: String)

  case class Node(word: String, cls: Int, cost: Int, pos: Int, tcost: Int = Int.MaxValue){

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
