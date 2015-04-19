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

import java.io.File
import java.nio.file.FileSystems
import org.apache.lucene.index._
import org.apache.lucene.search.TermQuery
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core._
import org.nlp4l.lm.HmmModelIndexer
import scala.io._
import scala.util.matching.Regex
import scalax.file.Path
import scalax.file.PathSet

val index = "/tmp/index-brown-hmm"

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// create HMM model index
val c: PathSet[Path] = Path("corpora", "brown", "brown").children()
val indexer = HmmModelIndexer(index)
c.filter{ e =>
  val s = e.name
  val c = s.charAt(s.length - 1)
  c >= '0' && c <= '9'
}.foreach{ f =>
  val source = Source.fromFile(f.path, "UTF-8")
  source.getLines().map(_.trim).filter(_.length > 0).foreach { g =>
    val pairs = g.split("\\s+")
    val doc = pairs.map{h =>
      val wc = h.split("/")
      (wc(0).toLowerCase(), wc(1))
    }
    indexer.addDocument(doc)
  }
}

indexer.close()
