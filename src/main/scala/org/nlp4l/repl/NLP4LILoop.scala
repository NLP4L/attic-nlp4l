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

import java.io.BufferedReader
import java.nio.file.FileSystems

import scala.tools.nsc.interpreter._

class NLP4LILoop(in0: Option[BufferedReader], override protected val out: JPrintWriter) extends ILoop{
  def this(in0: BufferedReader, out: JPrintWriter) = this(Some(in0), out)
  def this() = this(None, new JPrintWriter(Console.out, true))

  import LoopCommand.cmd

  lazy val nlp4lCommands = List(
    cmd("?", "[method]", "show help for NLP4L shell", help)
  )

  override def commands = super.commands ++ nlp4lCommands

  override def printWelcome(): Unit = {
    echo(
      s"""
         |Welcome to NLP4L!
         |Type in expressions to have them evaluated.
         |Type :help for more information
         |Type :? for information about NLP4L utilities
       """.trim.stripMargin)
  }

  override def prompt = "\nnlp4l> "

  def help(arg: String): Unit = {
    if (arg.isEmpty) {
      println("All utility methods for NLP4L shell. Type \":? <method>\" for more details.")
      println("-----------------------------------------------------------------------------")
      println("%-35s\t%s" format ("Method", "Brief Description"))
      println("-----------------------------------------------------------------------------")
      println("%-35s\t%s" format ("open(indexDir)", "open index"))
      println("%-35s\t%s" format ("close", "close index"))
      println("%-35s\t%s" format ("status", "display status / overview for current opened index"))
      println("%-35s\t%s" format ("browseTerms(field, [pageSize])", "start browsing terms(words) for the field"))
      println("%-35s\t%s" format ("topTerms([n])", "display top N (default is 10) frequent terms for the field [*1]"))
      println("%-35s\t%s" format ("nextTerms([skip])", "show next terms [*1]"))
      println("%-35s\t%s" format ("nt", "alias for \"nextTerms(1)\""))
      println("%-35s\t%s" format ("prevTerms([skip])", "show previous terms [*1]"))
      println("%-35s\t%s" format ("pt", "alias for \"prevTerms(1)\""))
      println("%-35s\t%s" format ("browseTermDocs(field, term, [pageSize])", "start browsing docs for the term in the specified field"))
      println("%-35s\t%s" format ("nextDocs([skip])", "show next docs [*2]"))
      println("%-35s\t%s" format ("nd", "alias for \"nextDocs(1)\""))
      println("%-35s\t%s" format ("prevDocs([skip])", "show previous docs (required to call this after \"browseTermDocs\" [*2]"))
      println("%-35s\t%s" format ("pd", "alias for \"prevDocs(1)\""))
      println("%-35s\t%s" format ("showDoc(docId, [fields])", "display (stored) field values for the document"))
      println("%-35s\t%s" format ("downloadLdcc", "download and extract Livedoor news corpus"))
      println("%-35s\t%s" format ("downloadBrown", "download and extract Brown corpus"))
      println("%-35s\t%s" format ("downloadReuters", "download and extract Reuters corpus"))
      println("[*1] required to call this after \"browseTerms\"")
      println("[*2] required to call this after \"browseTermDocs\"")
    } else {
      arg match {
        case "open" => {
          println("-- method signature --")
          println("def open(idxDir: String): RawReader")
          println("\n-- description --")
          println("Open Lucene index in the directory. If an index already opened, that is closed before the new index will be opened.")
          println("\n-- arguments --")
          println("%-10s\t%s" format ("idxDir", "Lucene index directory"))
          println("\n-- return value --")
          println("Return : index reader")
          println("\n-- usage --")
          println("nlp4l> open(\"/tmp/myindex\")")
        }
        case "close" => {
          println("-- method signature --")
          println("def close: Unit")
          println("\n-- description --")
          println("Close current opened directory.")
          println("\n-- usage --")
          println("nlp4l> close")
        }
        case "status" => {
          println("-- method signature --")
          println("def status: Unit")
          println("\n-- description --")
          println("Display status and overview information for current opened index.")
          println("\n-- requirements --")
          println("\"open\" must be called before calling this.")
          println("\n-- usage --")
          println("nlp4l> status")
        }
        case "browseTerms" => {
          println("-- method signature --")
          println("def browseTerms(fName: String, pageSize: Int = 20)")
          println("\n-- description --")
          println("Start browsing terms for the specified field. New terms pager is initialized.")
          println("\n-- requirements --")
          println("\"open\" must be called before calling this.")
          println("\n-- arguments --")
          println("%-10s\t%s" format ("fName", "field name"))
          println("%-10s\t%s" format ("pageSize", "max number of showing terms by \"nextTerms\" and \"prevTerms\" (page size)"))
          println("\n-- usage --")
          println("nlp4l> browseTerms(\"title\", 100)")
        }
        case "topTerms" => {
          println("-- method signature --")
          println("def topTerms(n: Int = 10)")
          println("\n-- description --")
          println("Display top N frequent terms for the field. Terms are ranked by Document Frequency (DF)")
          println("\n-- requirements --")
          println("\"browseTerms\" must be called before calling this.")
          println("\n-- arguments --")
          println("%-10s\t%s" format ("n", "max number of terms"))
          println("\n-- usage --")
          println("nlp4l> topTerms(50)")
        }
        case "nextTerms" => {
          println("-- method signature --")
          println("def nextTerms(skip: Int = 1)")
          println("\n-- description --")
          println("Show next terms. If skip is specified, these pages are skipped.")
          println("\n-- requirements --")
          println("\"browseTerms\" must be called before calling this.")
          println("\n-- arguments --")
          println("%-10s\t%s" format ("skip", "pages to be skipped"))
          println("\n-- usage --")
          println("nlp4l> nextTerms(10)")
        }
        case "nt" => {
          println("-- method signature --")
          println("def nt")
          println("\n-- description --")
          println("Alias for \"nextTerms(1)\". Type \":? nextTerms\" for more info.")
          println("\n-- usage --")
          println("nlp4l> nt")
        }
        case "prevTerms" => {
          println("-- method signature --")
          println("def prevTerms(skip: Int = 1)")
          println("\n-- description --")
          println("Show previous terms. If skip is specified, these pages are skipped.")
          println("\n-- requirements --")
          println("\"browseTerms\" must be called before calling this.")
          println("\n-- arguments --")
          println("%-10s\t%s" format ("skip", "pages to be skipped"))
          println("\n-- usage --")
          println("nlp4l> prevTerms(10)")
        }
        case "pt" => {
          println("-- method signature --")
          println("def pt")
          println("\n-- description --")
          println("Alias for \"prevTerms(1)\". Type \":? prevTerms\" for more info.")
          println("\n-- usage --")
          println("nlp4l> pt")
        }
        case "browseTermDocs" => {
          println("-- method signature --")
          println("def browseTermDocs(fName: String, text: String, pageSize: Int = 20)")
          println("\n-- description --")
          println("Start browsing docs for the term in the specified field. New term docs pager is initialized.")
          println("\n-- requirements --")
          println("\"open\" must be called before calling this.")
          println("\n-- arguments --")
          println("%-10s\t%s" format ("fName", "field name"))
          println("%-10s\t%s" format ("text", "term string"))
          println("%-10s\t%s" format ("pageSize", "max number of showing docs by \"nextDocs\" and \"prevDocs\" (page size)"))
          println("\n-- usage --")
          println("nlp4l> browseTermDocs(\"title\", \"lucene\", 100)")
        }
        case "nextDocs" => {
          println("-- method signature --")
          println("def nextDocs(skip: Int = 1)")
          println("\n-- description --")
          println("Show next docs. If skip is specified, these pages are skipped.")
          println("\n-- requirements --")
          println("\"browseTermDocs\" must be called before calling this.")
          println("\n-- arguments --")
          println("%-10s\t%s" format ("skip", "pages to be skipped"))
          println("\n-- usage --")
          println("nlp4l> nextDocs(10)")
        }
        case "nd" => {
          println("-- method signature --")
          println("def nd")
          println("\n-- description --")
          println("Alias for \"nextDocs(1)\". Type \":? nextDocs\" for more info.")
          println("\n-- usage --")
          println("nlp4l> nd")
        }
        case "prevDocs" => {
          println("-- method signature --")
          println("def prevDocs(skip: Int = 1)")
          println("\n-- description --")
          println("Show previous docs. If skip is specified, these pages are skipped.")
          println("\n-- requirements --")
          println("\"browseTermDocs\" must be called before calling this.")
          println("\n-- arguments --")
          println("%-10s\t%s" format ("skip", "pages to be skipped"))
          println("\n-- usage --")
          println("nlp4l> prevDocs(10)")
        }
        case "pd" => {
          println("-- method signature --")
          println("def pd")
          println("\n-- description --")
          println("Alias for \"prevDocs(1)\". Type \":? prevDocs\" for more info.")
          println("\n-- usage --")
          println("nlp4l> pd")
        }
        case "showDoc" => {
          println("-- method signature --")
          println("def showDoc(docId: Int, fields: List[String] = List.empty)")
          println("\n-- description --")
          println("Show the stored field values for the document specified by docId. If fields parameter is not specified, all field values are showed.")
          println("\n-- requirements --")
          println("\"open\" must be called before calling this.")
          println("\n-- arguments --")
          println("%-10s\t%s" format ("docId", "Lucene's internal document id"))
          println("%-10s\t%s" format ("fields", "List of fields to be showed."))
          println("\n-- usage --")
          println("nlp4l> showDoc(150)")
        }
        case "downloadLdcc" => {
          println("-- method signature --")
          println("def downloadLdcc()")
          println("\n-- description --")
          println("Download and extract Livedoor news corpus from: http://www.rondhuit.com/download/ldcc-20140209.tar.gz\"")
          println("Downloaded archive and extracted corpus is placed to corpora/ldcc.")
          println("\n-- requirements --")
          println("\n-- usage --")
          println("nlp4l> downloadLdcc")
        }
        case "downloadBrown" => {
          println("-- method signature --")
          println("def downloadBrown()")
          println("\n-- description --")
          println("Download and extract Brown corpus from: https://ia600503.us.archive.org/21/items/BrownCorpus/brown.zip")
          println("Downloaded archive and extracted corpus is placed to corpora/brown.")
          println("\n-- requirements --")
          println("\n-- usage --")
          println("nlp4l> downloadBrown")
        }
        case "downloadReuters" => {
          println("-- method signature --")
          println("def downloadReuters()")
          println("\n-- description --")
          println("Download and extract Reuters corpus from: http://www.daviddlewis.com/resources/testcollections/reuters21578/reuters21578.tar.gz")
          println("Downloaded archive and extracted corpus is placed to corpora/reuters.")
          println("\n-- requirements --")
          println("\n-- usage --")
          println("nlp4l> downloadReuters")
        }
        case unknown => {
          println("Unknown method %s." format unknown)
          println("Please check methods overview. Type \":?\"")
        }
      }
    }
  }
}
