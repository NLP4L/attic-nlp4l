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

package org.nlp4l.gui

import java.io.File
import java.io.IOException
import java.io.OutputStream

object BarChart {
  def apply(model: List[(String,Map[String,Long])]) = new BarChart(model)
  def apply(model: (String,Map[String,Long])) = new BarChart(List(model))
}

class BarChart(model: List[(String,Map[String,Long])]) extends Presentation {

  val labelList = model.head._2.keys.toList.sorted

  val labels = {
    labelList.map("\"%s\"".format(_)).mkString(",")
  }

  val datasets = {
    var i = -1
    model.map { m =>
      i += 1
      """{label:"%s",fillColor:"%s",strokeColor:"%s",data:[%s]}""".
        format(m._1,fillColor(i), strokeColor(i), dataset(m._2))
    }.mkString(",")
  }

  def fillColor(idx: Int): String = {
    idx match {
      case 0 => "rgba(228,150,66,1)"
      case 1 => "rgba(245,88,86,1)"
      case 2 => "rgba(83,167,157,1)"
      case 3 => "rgba(39,74,98,1)"
      case _ => "rgba(226,190,160,1)"
    }
  }

  def strokeColor(idx: Int): String = {
    idx match {
      case 0 => "rgba(228,150,66,1)"
      case 1 => "rgba(245,88,86,1)"
      case 2 => "rgba(83,167,157,1)"
      case 3 => "rgba(39,74,98,1)"
      case _ => "rgba(226,190,160,1)"
    }
  }

  def dataset(m: Map[String,Long]): String = {
    labelList.map(m.getOrElse(_, 0)).mkString(",")
  }

  val LEGEND_TEMPLATE = """legendTemplate : "<% for (var i=0; i<datasets.length; i++){%><li><span style=\"color:<%=datasets[i].strokeColor%>\">■</span><%if(datasets[i].label){%><%=datasets[i].label%><%}%></li><%}%>""""
  val TEST_HTML =
    """
      |<html>
      |  <head>
      |    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
      |    <title>use of Chart.js</title>
      |    <script src="src/main/resources/js/Chart.js"></script>
      |  </head>
      |  <body>
      |    <canvas id="sample" height="200" width="400"></canvas>
      |    <ul id="legend" style="list-style:none"></ul>
      |<script>
      |var barChartData = {
      |  labels : [%s],
      |  datasets : [%s]
      |}
      |var chartOption = { %s }
      |var chart = new Chart(document.getElementById("sample").getContext("2d")).Bar(barChartData,chartOption);
      |document.getElementById('legend').innerHTML = chart.generateLegend();
      |</script>
      |  </body>
      |</html>
      |""".stripMargin.format(labels, datasets, LEGEND_TEMPLATE).getBytes()

  override def length(): Int = { TEST_HTML.size }

  override def execute(path: String, out: OutputStream): Unit = {
    TEST_HTML.foreach(out.write(_))
  }
}
