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

class HmmTokenizer(model: HmmModel) extends HmmTracer {

  var nodesTableByEnd = scala.collection.mutable.Map.empty[Int, Node]
  val debug = false

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
    val leftNode = nodesTableByEnd.getOrElse(pos, null)
    if(leftNode == null && pos < str.length){
      parseForward(str, pos + 1, EOS)
    }
    else{
      if(pos == str.length){
        processLeftLink(model, leftNode, EOS)
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
            processLeftLink(model, leftNode, node)
          }
        }
        parseForward(str, pos + 1, EOS)
      }
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

  def createToken(str: String, node: AbstractNode): Token = {
    Token(str.substring(node.asInstanceOf[Node].spos, node.asInstanceOf[Node].epos), model.className(node.cls))
  }

  object Node {
    def apply(cls: Int, cost: Int, spos: Int, epos: Int, tcost: Int = Int.MaxValue) = new Node(cls, cost, spos, epos, tcost)
  }

  class Node(cls: Int, cost: Int, val spos: Int, val epos: Int, tcost: Int = Int.MaxValue) extends AbstractNode(cls, cost, tcost)
}
