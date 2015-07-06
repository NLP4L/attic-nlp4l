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

package org.nlp4l.repl

object ZeppelinVisualizer {

  def table(args: Any, labels: String*): String = {
    val header = "%%table %s\n".format(
      labels.mkString("\t")
    )
    args match {
      case a: Map[_, _] => {
        val records = a.map(b => "%s\t%f".format(b._1, b._2.toString.toFloat)).mkString("\n")
        header + records
      }
      case a: Array[(_, _, _)] => {
        val records = a.map(b => "%s\t%f\t%f".format(b._1, b._2.toString.toFloat, b._3.toString.toFloat)).mkString("\n")
        header + records
      }
      case a: Array[(_, _)] => {
        val records = a.map(b => "%s\t%f".format(b._1, b._2.toString.toFloat)).mkString("\n")
        header + records
      }
      case _ => { "not supported type %s".format(args) }
    }
  }
}
