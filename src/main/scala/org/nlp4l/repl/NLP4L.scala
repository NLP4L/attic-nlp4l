package org.nlp4l.repl

import java.nio.file._

import org.nlp4l.core._
import org.nlp4l.core.analysis._

/**
 * REPL起動時に読み込まれるObject
 */
object NLP4L {
  private var _reader: RawReader = null
  private var field: FieldInfo = null
  private var term: TermDocs = null
  private var _nextTerms: () => List[TermDocs] = null
  private var _prevTerms: () => List[TermDocs] = null
  private var _nextDocs: () => List[Doc] = null
  private var _prevDocs: () => List[Doc] = null

  def reader = _reader

  def showDoc(docId: Int, fields: List[String] = List.empty) = {
    if (reader == null)
      println("No index opened.")
    else {
      reader.document(docId) match {
        case Some(doc) => {
          val showFields = if (fields.isEmpty) reader.fieldNames else fields
          println("Doc #" + docId)
          doc.fields.filter(f => showFields.contains(f.name))
            .foreach(f => (println("(Field) " + f.name + ": " + f.values.mkString("[", ",", "]"))))
        }
        case None => println("Doc #" + docId + " <no such doc>")
      }
    }
  }

  def browseTerms(fName: String, pageSize: Int = 20) = {
    reader.field(fName) match {
      case Some(field) => {
        this.field = field
        val (next, prev) = termsPager(pageSize)
        _nextTerms = next
        _prevTerms = prev
        println("Browse terms for field %s, page size %d".format(field.name, pageSize))
        println("Type \"nextTerms(skip)\" or \"nt\" to browse next terms.")
        println("Type \"prevTerms(skip)\" or \"pt\" to browse prev terms.")
        println("Type \"topTerms(n)\" to find top n frequent terms.")
      }
      case _ => println("[ERROR] No such field in index: %s".format(fName))
    }
  }

  def browseTermDocs(fName: String, text: String, pageSize: Int = 20) = {
    reader.field(fName) match {
      case Some(field) => field.term(text) match {
        case Some(term) => {
          this.field = field
          this.term = term
          val (next, prev) = docsPager(pageSize)
          _nextDocs = next
          _prevDocs = prev
          println("Browse docs for term %s in field %s, page size %d".format(field.name, term.text, pageSize))
          println("Type \"nextDocs(skip)\" or \"nd\" to browse next terms.")
          println("Type \"prevDocs(skip)\" or \"pd\" to browse prev terms.")
        }
        case _ => println("[ERROR] No such term in field %s: %s".format(fName, text))
      }
      case _ => println("[ERROR] No such field in index: %s".format(fName))
    }
  }

  def nextTerms(skip: Int = 1) = {
    if (reader == null)
      println("No index opened.")
    else if (field == null)
      println("No field passed. Please call examine(<fieldName>)")
    else {
      for (i <- 1 to skip - 1) _nextTerms()
      println("Indexed terms for field '%s'".format(field.name))
      val res: List[TermDocs] = _nextTerms()
      res.foreach(td => {
        println("%s (DF=%d, Total TF=%d)".format(td.text, td.docFreq, td.totalTermFreq))
      })
    }
  }

  // alias method
  def nt = nextTerms(1)

  def prevTerms(skip: Int = 1) = {
    if (reader == null)
      println("No index opened.")
    else if (field == null)
      println("No field passed. Please call examine(<fieldName>)")
    else {
      for (i <- 1 to skip - 1) _prevTerms()
      println("Indexed terms for field '%s'".format(field.name))
      val res: List[TermDocs] = _prevTerms()
      res.foreach(td => {
        println("%s (DF=%d, Total TF=%d)".format(td.text, td.docFreq, td.totalTermFreq))
      })
    }
  }

  // alias method
  def pt = prevTerms(1)

  def topTerms(n: Int = 10) = {
    if (reader == null)
      println("No index opened.")
    else if (field == null)
      println("No field passed. Please call examine(<fieldName>)")
    else {
      println("Top %d frequent terms for field %s".format(n, field.name))
      for ((t, i) <- reader.topTermsByDocFreq(field.name, n).zipWithIndex) {
        println("%3d: %s (DF=%d, Total TF=%d)".format(i+1, t._1, t._2, t._3))
      }
    }
  }


  def nextDocs(skip: Int = 1) = {
    if (reader == null)
      println("No index opened.")
    else if (field == null || term == null)
      println("No term passed. Please call examine(<fieldName>, <term>)")
    else {
      for (i <- 1 to skip - 1) _nextDocs()
      println("Documents for term '%s' in field '%s'".format(term.text, field.name))
      val  res: List[Doc] = _nextDocs()
      res.foreach(d => {
        println(d.toString)
      })
    }
  }

  // alias method
  def nd = nextDocs(1)

  def prevDocs(skip: Int = 1) = {
    if (reader == null)
      println("No index opened.")
    else if (field == null || term == null)
      println("No term passed. Please call examine(<fieldName>, <term>)")
    else {
      for (i <- 1 to skip - 1) _prevDocs()
      println("Documents for term '%s' in field '%s'".format(term.text, field.name))
      val res: List[Doc] = _prevDocs()
      res.foreach(d => {
        println(d.toString + " positions: " + d.posAndOffsets.mkString("[", ",", "]"))
      })
    }
  }

  // alias method
  def pd = prevDocs(1)

  def open(idxDir: String): RawReader = {
    close
    val path = FileSystems.getDefault.getPath(idxDir)
    _reader = RawReader(path)
    println("Index " + reader.path + " was opened.")
    reader
  }

  def close: Unit = {
    if (reader == null)
      return
    if (reader.closed) {
      println("Index " + reader.path + " has been already closed.")
    } else {
      reader.close
      println("Index " + reader.path + " was closed.")
    }
  }

  def status: Unit = {
    if (reader == null) {
      println("No index opened.")
    } else {
      displayIndexInfo(reader)
      displayFieldsInfo(reader)
    }
  }

  private def displayIndexInfo(r: RawReader): Unit = {
    if (r == null)
      println("No index passed.")
    else {
      val border = "=" * 40
      println(
        """
          |%s
          |Index Path       : %s
          |Closed           : %s
          |Num of Fields    : %d
          |Num of Docs      : %d
          |Num of Max Docs  : %d
          |Has Deletions    : %s
          |%s
        """.stripMargin
          .format(
              border,
              r.path, r.closed, r.fields.length, r.numDocs, r.maxDoc,
              r.hasDeletions + (if (r.hasDeletions) " (" + r.numDeletedDocs + ")" else ""),
              border))
    }
  }

  private def displayFieldsInfo(r: RawReader = reader): Unit = {
    if (r == null) {
      println("No index passed.")
    } else if (r.fields == null || r.fields.isEmpty)
      println("No fields.")
    else {
      println("Fields Info:")

      val border = "=" * 40
      val separator = "-" * 40
      println(border)

      val maxlen = r.fieldNames.maxBy(n => n.length).length
      val nameColLen = math.max(maxlen, 4)
      println("  # " +  "| Name" + " " * (nameColLen - 4) + " | Num Terms ")
      println(separator)
      r.fields.foreach(f => {
        println("%3d | %s | %10d".format(f.number, f.name + " " * (nameColLen - f.name.length), f.uniqTerms))
      })
      println(border)
    }
  }

  // term のブラウジング用のクロージャ next, prev を作る関数
  private def termsPager(pageSize:Int = 20): (() => List[TermDocs], () => List[TermDocs]) = {
    var cursor, maxCursor = 0
    var nextTerms: Iterable[TermDocs] = field.terms
    var prevTerms: List[TermDocs] = List.empty[TermDocs]

    def next() = {
      val ret =
        if (cursor > maxCursor) {
          cursor = maxCursor // (current) cursor should not exceed maxCursor
          List.empty[TermDocs]
        } else if (maxCursor == cursor) {
          val _ret = nextTerms.take(pageSize).toList
          nextTerms = nextTerms.drop(pageSize)
          prevTerms = prevTerms ++ _ret
          maxCursor += _ret.length
          _ret
        } else {
          val _ret = prevTerms.take(cursor + pageSize).takeRight(pageSize).toList
          _ret
        }
      cursor += ret.length
      ret
    }

    def prev() = {
      val ret =
        if (cursor <= 0) {
          cursor = 0  // cursor should not be negative value
          List.empty[TermDocs]
        } else {
          val _ret = prevTerms.take(cursor).takeRight(pageSize).toList
          cursor -= _ret.length
          _ret
        }
      ret
    }

    (next, prev)
  }

  // doc のブラウジング用のクロージャ next, prev を作る関数
  private def docsPager(pageSize: Int = 20): (() => List[Doc], () => List[Doc]) ={
    var cursor, maxCursor = 0
    var nextDocs: Iterable[Doc] = term
    var prevDocs: List[Doc] = List.empty[Doc]

    def next() = {
      val ret =
      if (cursor > maxCursor) {
        cursor = maxCursor  // (current) cursor should not exceed maxCursor
        List.empty[Doc]
      } else if (maxCursor == cursor) {
        val _ret = nextDocs.take(pageSize).toList
        nextDocs = nextDocs.drop(pageSize)
        prevDocs = prevDocs ++ _ret
        maxCursor += _ret.length
        _ret
      } else {
        val _ret = prevDocs.take(cursor + pageSize).takeRight(pageSize).toList
        _ret
      }
      cursor += ret.length
      ret
    }

    def prev() = {
      val ret =
      if (cursor <= 0) {
        cursor = 0  // cursor should not be negative value
        List.empty[Doc]
      } else {
        val _ret = prevDocs.take(cursor).takeRight(pageSize).toList
        cursor -= _ret.length
        _ret
      }
      ret
    }

    (next, prev)
  }
}
