package org.nlp4l.lm

class StringAligner(){

  val PATH_FROM_UPPER = 1
  val PATH_FROM_UPPER_LEFT = 2
  val PATH_FROM_LEFT = 3
  val targetGatherRules: List[(String, String)] = List(
    ("ā", "ear"), ("ā", "ar"), ("ā", "er"), ("ā", "ir"), ("ā", "or"), ("ā", "ur"),
    ("a", "er"),
    ("ē", "ai"),
    ("ī", "ey"), ("ī", "ee"),
    ("ū", "oo"),
    ("ō", "or"), ("ō", "oa"), ("ō", "ar"), ("ō", "au"), ("ō", "ow"),
    ("k", "ch"),
    ("t", "tt"), ("p", "pp"), ("m", "mm"), ("f", "ff"), ("h", "ff"), ("r", "ll")
  )
  val sourceGatherRules: List[(String, String)] = List(
    ("ai", "y"),
    ("ru", "l"),
    ("kk", "c"), ("ku", "c"),  // this order is important!
    ("ku", "x"),
    ("ei", "a"), ("fu", "f"), ("pu", "p"), ("bu", "v"),
    ("tt", "t"), ("pp", "p"),
    ("u~", "w")
  )
  val tailOnlySourceGatherRules: List[(String, String)] = List(
    ("to", "t"), ("mu", "m"), ("do", "d"), ("gu", "g")
  )

  def align(source: String, target: String, debug: Boolean = false): Seq[(String, String)] = {
    val sl1 = source.length + 1
    val tl1 = target.length + 1
    val scoreTable: Array[Array[Int]] = new Array[Array[Int]](sl1)

    // initialize score table
    for(i <- 1 to sl1){
      scoreTable(i - 1) = new Array[Int](tl1)
      if(i == 1){
        for(j <- 1 to tl1){
          scoreTable(i - 1)(j - 1) = 0
        }
      }
      else{
        scoreTable(i - 1)(0) = 0
      }
    }

    // make alignment
    val pathTable: Array[Array[Int]] = new Array[Array[Int]](sl1 - 1)
    val windowAllowance = (target.length.toFloat / source.length.toFloat * 2).toInt

    for(si <- 0 to sl1 - 2){
      pathTable(si) = new Array[Int](tl1 - 1)
      for(ti <- 0 to tl1 - 2){
        pathTable(si)(ti) = applyGatherRules(findPath(scoreTable, si + 1, ti + 1, source.charAt(si), target.charAt(ti), windowAllowance),
          si, ti, source, target)
      }
    }

    // dump tables for debug
    if(debug){
      val arrows: Array[String] = Array("↑", "↖", "←")
      print("      ")
      for(ti <- 0 to tl1 - 2){
        print("  %2s".format(target.charAt(ti).toString))
      }
      println
      print("  ")
      for(ti <- 0 to tl1 - 1){
        print("  %2d".format(0))
      }
      println
      for(si <- 1 to sl1 - 1){
        print("%2s".format(source.charAt(si - 1).toString))
        print("  %2d".format(0))
        for(ti <- 1 to tl1 - 1){
          val arrow = arrows(pathTable(si - 1)(ti - 1) - 1)
          print("%2s%2d".format(arrow, scoreTable(si)(ti)))
        }
        println
      }
    }

    // back trace
    backTrace(pathTable, sl1 - 2, tl1 - 2, source, target)
  }

  /**
   * Find the best path and return the best path number, and set the score of the cell of scoreTable.
   * @param scoreTable 2 dimensional score table
   * @param si source index of the score table (starts with 0)
   * @param ti target index of the score table (starts with 0)
   * @param sc source character
   * @param tc target character
   * @param windowAllowance allowance for the window size
   * @return the best path number. 1 for upper, 2 for upper left, 3 for left
   */
  def findPath(scoreTable: Array[Array[Int]], si: Int, ti: Int, sc: Character, tc: Character, windowAllowance: Int): Int = {
    val upperScore = scoreTable(si - 1)(ti)
    val upperLeftScore = scoreTable(si - 1)(ti - 1)
    val leftScore = scoreTable(si)(ti - 1)
    if(inWindow(si - 1, ti -1, windowAllowance) && sc == tc){
      val tempScore = upperLeftScore + 1
      if(upperScore > tempScore){
        if(upperScore > leftScore){
          scoreTable(si)(ti) = upperScore
          PATH_FROM_UPPER
        }
        else{
          scoreTable(si)(ti) = leftScore
          PATH_FROM_LEFT
        }
      }
      else if(leftScore > tempScore){
        if(upperScore > leftScore){
          scoreTable(si)(ti) = upperScore
          PATH_FROM_UPPER
        }
        else{
          scoreTable(si)(ti) = leftScore
          PATH_FROM_LEFT
        }
      }
      else{
        scoreTable(si)(ti) = tempScore
        PATH_FROM_UPPER_LEFT
      }
    }
    else{
      if(upperScore > upperLeftScore){
        if(upperScore > leftScore){
          scoreTable(si)(ti) = upperScore
          PATH_FROM_UPPER
        }
        else{
          scoreTable(si)(ti) = leftScore
          PATH_FROM_LEFT
        }
      }
      else if(leftScore > upperLeftScore){
        if(upperScore > leftScore){
          scoreTable(si)(ti) = upperScore
          PATH_FROM_UPPER
        }
        else{
          scoreTable(si)(ti) = leftScore
          PATH_FROM_LEFT
        }
      }
      else{
        scoreTable(si)(ti) = upperLeftScore
        if(si == 1 && ti > 1) PATH_FROM_LEFT
        else if(si > 1 && ti == 1) PATH_FROM_UPPER
        else PATH_FROM_UPPER_LEFT
      }
    }
  }

  def inWindow(si: Int, ti: Int, windowAllowance: Int): Boolean = {
    (si - windowAllowance) <= ti && ti <= (si + windowAllowance)
  }

  def backTrace(pathTable: Array[Array[Int]], si: Int, ti: Int, source: String, target: String): Seq[(String, String)] = {
    val result = scala.collection.mutable.ArrayBuffer.empty[(String, String)]
    backTrace(result, pathTable, si, ti, List.empty[Char], List.empty[Char], source, target)
  }

  /**
   *
   * @param result
   * @param pathTable
   * @param si source index for pathTable and source String
   * @param ti target index for pathTable and target String
   * @param sStack
   * @param tStack
   * @param source source String
   * @param target target String
   * @return
   */
  def backTrace(result: scala.collection.mutable.ArrayBuffer[(String, String)], pathTable: Array[Array[Int]],
                si: Integer, ti: Integer, sStack: List[Char], tStack: List[Char], source: String, target: String): Seq[(String, String)] = {
    if(si < 0){
      if(ti < 0){
        //result += Pair(sStack.mkString, tStack.mkString)   // commented out because this produces an empty tuple
        result.toSeq.reverse
      }
      else{
        backTrace(result, pathTable, si, ti - 1, sStack, target.charAt(ti) :: tStack, source, target)
      }
    }
    else{
      if(ti < 0){
        backTrace(result, pathTable, si - 1, ti, source.charAt(si) :: sStack, tStack, source, target)
      }
      else{
        pathTable(si)(ti) match {
          case PATH_FROM_UPPER => {
            backTrace(result, pathTable, si - 1, ti, source.charAt(si) :: sStack, tStack, source, target)
          }
          case PATH_FROM_LEFT => {
            backTrace(result, pathTable, si, ti - 1, sStack, target.charAt(ti) :: tStack, source, target)
          }
          case _ => {
            result += Pair((source.charAt(si) :: sStack).mkString, (target.charAt(ti) :: tStack).mkString)
            backTrace(result, pathTable, si - 1, ti - 1, List.empty, List.empty, source, target)
          }
        }
      }
    }
  }

  def applyGatherRules(path: Int, si: Int, ti: Int, source: String, target: String): Int = {
    var modPath = path

    targetGatherRules.foreach{ rule =>
      if(modPath == PATH_FROM_UPPER_LEFT){   // do not move this if statement out of foreach!
        modPath = stringsMatch(rule, si, ti, source, target, PATH_FROM_LEFT, PATH_FROM_UPPER_LEFT)
      }
    }

    sourceGatherRules.foreach{ rule =>
      if(modPath == PATH_FROM_UPPER_LEFT){   // do not move this if statement out of foreach!
        modPath = stringsMatch(rule, si, ti, source, target, PATH_FROM_UPPER, PATH_FROM_UPPER_LEFT)
      }
    }

    if(si == source.length - 1 && ti == target.length -1){
      tailOnlySourceGatherRules.foreach{ rule =>
        modPath = stringsMatch(rule, si, ti, source, target, PATH_FROM_UPPER, modPath)
      }
    }

    modPath
  }

  def stringsMatch(rule: (String, String), si: Int, ti: Int, source: String, target: String, matchPath: Int, defPath: Int): Int = {
    if(si + 1 < rule._1.length || ti + 1 < rule._2.length) defPath
    else{
      val ssub = source.substring(si + 1 - rule._1.length, si + 1)
      val tsub = target.substring(ti + 1 - rule._2.length, ti + 1)
      //println("%s,%s : %s,%s".format(ssub, tsub, rule._1, rule._2))
      if(ssub == rule._1 && tsub == rule._2) matchPath
      else defPath
    }
  }
}
