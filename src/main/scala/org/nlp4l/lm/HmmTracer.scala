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

abstract class HmmTracer {

  val CLASS_BOS = -2
  val CLASS_EOS = -2

  def tokens(str: String): Seq[Token]

  protected def processLeftLink(leftNode: AbstractNode, rightNode: AbstractNode): Unit = {
    if(leftNode != null){
      rightNode.replaceTotalCostIfSmaller(leftNode)
      processLeftLink(leftNode.nextSameEnd, rightNode)
    }
  }

  def createToken(str: String, node: AbstractNode): Token

  def backTrace(str: String, node: AbstractNode): Seq[Token] = {
    val buf = scala.collection.mutable.ArrayBuffer.empty[Token]
    backTrace(str, node, buf)
  }

  def backTrace(str: String, node: AbstractNode, buf: scala.collection.mutable.ArrayBuffer[Token]): Seq[Token] = {
    if(node.cls == CLASS_BOS){
      buf.toList.reverse
    }
    else{
      val token = createToken(str, node)
      buf += token
      backTrace(str, node.backLink, buf)
    }
  }
}

case class Token(word: String, cls: String)

abstract case class AbstractNode(cls: Int, cost: Int, tcost: Int = Int.MaxValue){

  var backLink: AbstractNode = null
  var total: Int = tcost
  var nextSameEnd: AbstractNode = null

  def replaceTotalCostIfSmaller(leftNode: AbstractNode): Unit = {
    if(leftNode.total + cost < total){
      backLink = leftNode
      total = leftNode.total + cost
    }
  }
}
