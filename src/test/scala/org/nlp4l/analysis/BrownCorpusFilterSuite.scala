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

package org.nlp4l.core.analysis

import org.scalatest.FunSuite

class BrownCorpusFilterSuite extends FunSuite with AnalyzerAsserts {

  test("no POS BrownCorpusFilter test") {
    val a = Analyzer(new org.apache.lucene.analysis.brown.BrownCorpusAnalyzer)
    val results = a.tokens("The Fulton County Grand Jury said Friday an/at investigation of/in Atlanta's/np$ recent primary election produced `` no evidence '' .").toArray
    assertToken(results(0), Map("term" -> "the",       "partOfSpeech" -> "*no value*"))
    assertToken(results(1), Map("term" -> "fulton",    "partOfSpeech" -> "*no value*"))
    assertToken(results(2), Map("term" -> "county",    "partOfSpeech" -> "*no value*"))
    assertToken(results(3), Map("term" -> "grand",     "partOfSpeech" -> "*no value*"))
    assertToken(results(4), Map("term" -> "jury",      "partOfSpeech" -> "*no value*"))
    assertToken(results(5), Map("term" -> "said",      "partOfSpeech" -> "*no value*"))
    assertToken(results(6), Map("term" -> "friday",    "partOfSpeech" -> "*no value*"))
    assertToken(results(7), Map("term" -> "an",        "partOfSpeech" -> "at"))
    assertToken(results(8), Map("term" -> "investigation", "partOfSpeech" -> "*no value*"))
    assertToken(results(9), Map("term" -> "of",        "partOfSpeech" -> "in"))
    assertToken(results(10), Map("term" -> "atlanta's", "partOfSpeech" -> "np$"))
    assertToken(results(11), Map("term" -> "recent",   "partOfSpeech" -> "*no value*"))
    assertToken(results(12), Map("term" -> "primary",  "partOfSpeech" -> "*no value*"))
    assertToken(results(13), Map("term" -> "election", "partOfSpeech" -> "*no value*"))
    assertToken(results(14), Map("term" -> "produced", "partOfSpeech" -> "*no value*"))
    assertToken(results(15), Map("term" -> "``",       "partOfSpeech" -> "*no value*"))
    assertToken(results(16), Map("term" -> "no",       "partOfSpeech" -> "*no value*"))
    assertToken(results(17), Map("term" -> "evidence", "partOfSpeech" -> "*no value*"))
    assertToken(results(18), Map("term" -> "''",       "partOfSpeech" -> "*no value*"))
    assertToken(results(19), Map("term" -> ".",        "partOfSpeech" -> "*no value*"))
  }

  test("simple full POS BrownCorpusFilter test") {
    val a = Analyzer(new org.apache.lucene.analysis.brown.BrownCorpusAnalyzer)
    //                                1         2         3         4         5         6         7         8         9         0         1         2         3         4         5         6         7         8         9
    //                      012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567
    val results = a.tokens("The/at Fulton/np-tl County/nn-tl Grand/jj-tl Jury/nn-tl said/vbd Friday/nr an/at investigation/nn of/in Atlanta's/np$ recent/jj primary/nn election/nn produced/vbd ``/`` no/at evidence/nn ''/'' ./.").toArray
    assertToken(results(0), Map("term" -> "the",       "partOfSpeech" -> "at",    "endOffset" -> "3"))
    assertToken(results(1), Map("term" -> "fulton",    "partOfSpeech" -> "np-tl", "endOffset" -> "13"))
    assertToken(results(2), Map("term" -> "county",    "partOfSpeech" -> "nn-tl", "endOffset" -> "26"))
    assertToken(results(3), Map("term" -> "grand",     "partOfSpeech" -> "jj-tl", "endOffset" -> "38"))
    assertToken(results(4), Map("term" -> "jury",      "partOfSpeech" -> "nn-tl", "endOffset" -> "49"))
    assertToken(results(5), Map("term" -> "said",      "partOfSpeech" -> "vbd",   "endOffset" -> "60"))
    assertToken(results(6), Map("term" -> "friday",    "partOfSpeech" -> "nr",    "endOffset" -> "71"))
    assertToken(results(7), Map("term" -> "an",        "partOfSpeech" -> "at",    "endOffset" -> "77"))
    assertToken(results(8), Map("term" -> "investigation", "partOfSpeech" -> "nn", "endOffset" -> "94"))
    assertToken(results(9), Map("term" -> "of",        "partOfSpeech" -> "in",    "endOffset" -> "100"))
    assertToken(results(10), Map("term" -> "atlanta's", "partOfSpeech" -> "np$",  "endOffset" -> "113"))
    assertToken(results(11), Map("term" -> "recent",   "partOfSpeech" -> "jj",    "endOffset" -> "124"))
    assertToken(results(12), Map("term" -> "primary",  "partOfSpeech" -> "nn",    "endOffset" -> "135"))
    assertToken(results(13), Map("term" -> "election", "partOfSpeech" -> "nn",    "endOffset" -> "147"))
    assertToken(results(14), Map("term" -> "produced", "partOfSpeech" -> "vbd",   "endOffset" -> "159"))
    assertToken(results(15), Map("term" -> "``",       "partOfSpeech" -> "``",    "endOffset" -> "166"))
    assertToken(results(16), Map("term" -> "no",       "partOfSpeech" -> "at",    "endOffset" -> "172"))
    assertToken(results(17), Map("term" -> "evidence", "partOfSpeech" -> "nn",    "endOffset" -> "184"))
    assertToken(results(18), Map("term" -> "''",       "partOfSpeech" -> "''",    "endOffset" -> "190"))
    assertToken(results(19), Map("term" -> ".",        "partOfSpeech" -> ".",     "endOffset" -> "195"))
  }
}
