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

import java.io._
import java.nio.file.{Path, Files, FileSystems}

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

/**
 * Utility object for handling corpus
 */
object Corpora {

  val CORPORA_ROOT = "corpora"

  // corpora identifiers
  val LDCC = "ldcc"
  val BROWN = "brown"
  val REUTERS = "reuters"

  // corpora locations and download directories
  val corpora = Map(
    LDCC -> ("http://www.rondhuit.com/download/", "ldcc-20140209.tar.gz", FileSystems.getDefault.getPath(CORPORA_ROOT, LDCC)),
    BROWN -> ("https://ia600503.us.archive.org/21/items/BrownCorpus/", "brown.zip", FileSystems.getDefault.getPath(CORPORA_ROOT, BROWN)),
    REUTERS -> ("http://www.daviddlewis.com/resources/testcollections/reuters21578/", "reuters21578.tar.gz", FileSystems.getDefault.getPath(CORPORA_ROOT, REUTERS))
  )

  def downloadAndExtract(url: String, file: String, path: Path): Unit = {

    def execSysCmd(cmd: String): Unit = {
      println("Try to execute system command: %s".format(cmd))
      val p = Runtime.getRuntime.exec(cmd)
      if (p.waitFor() != 0) println("Execute failed.")
      else println("Success.")
    }

    // path to save archive
    val target = FileSystems.getDefault.getPath(path.toAbsolutePath.toString, file)

    if (!Files.exists(path)) {
      Files.createDirectories(path)
    } else if (!Files.isDirectory(path)) {
      println("[ERROR] File " + path.toAbsolutePath.toString + " already exists, but not a directory.")
      System.exit(1)
    } else {
      Files.deleteIfExists(target)
    }

    // download archive file from corpus's location
    val client = HttpClients.createDefault()
    val httpGet = new HttpGet(url + file)
    val response = client.execute(httpGet)

    try {
      val entity = response.getEntity
      Files.copy(entity.getContent, FileSystems.getDefault.getPath(path.toAbsolutePath.toString, file))
      EntityUtils.consume(entity)
      println("Successfully downloaded " + file)

      // extract archive
      val archive = new File(target.toAbsolutePath.toString)
      if (file.endsWith(".tgz") || file.endsWith(".tar.gz"))
        execSysCmd("tar xzf %s -C %s".format(archive, path.toAbsolutePath.toString))
      else if (file.endsWith(".zip"))
        execSysCmd("unzip -o %s -d %s".format(archive, path.toAbsolutePath.toString))
    } finally {
      response.close()
    }
  }

  /**
   * download and extract Livedoor news corpus
   * http://www.rondhuit.com/download/ldcc-20140209.tar.gz
   */
  def downloadLdcc() = {
    val corpus = corpora(LDCC)
    downloadAndExtract(corpus._1, corpus._2, corpus._3)
  }

  /**
   * download and extract Brown corpus
   * https://ia600503.us.archive.org/21/items/BrownCorpus/brown.zip
   */
  def downloadBrown() = {
    val corpus = corpora(BROWN)
    downloadAndExtract(corpus._1, corpus._2, corpus._3)
  }

  /**
   * download and extract Reuters corpus
   * http://www.daviddlewis.com/resources/testcollections/reuters21578/reuters21578.tar.gz
   */
  def downloadReuters() = {
    val corpus = corpora(REUTERS)
    downloadAndExtract(corpus._1, corpus._2, corpus._3)
  }
}
