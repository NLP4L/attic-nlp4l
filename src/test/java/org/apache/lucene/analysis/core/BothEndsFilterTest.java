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

package org.apache.lucene.analysis.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerAssertions;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.junit.Test;

public class BothEndsFilterTest extends AnalyzerAssertions {

  @Test
  public void testNormal() throws Exception {
    // bi-gram
    assertTokens(new BothEndsAnalyzer(2), "a bb ccc dddd eeeee ffffff",
            "a bb", "bb ccc", "ccc dddd", "dddd eeeee", "eeeee ffffff");

    // tri-gram
    assertTokens(new BothEndsAnalyzer(3), "a bb ccc dddd eeeee ffffff",
            "a ccc", "bb dddd", "ccc eeeee", "dddd ffffff");

    // 4-gram
    assertTokens(new BothEndsAnalyzer(4), "a bb ccc dddd eeeee ffffff",
            "a dddd", "bb eeeee", "ccc ffffff");
  }

  @Test
  public void testReverse() throws Exception {
    // bi-gram
    assertTokens(new BothEndsAnalyzer(2, true), "a bb ccc dddd eeeee ffffff",
            "bb a", "ccc bb", "dddd ccc", "eeeee dddd", "ffffff eeeee");

    // tri-gram
    assertTokens(new BothEndsAnalyzer(3, true), "a bb ccc dddd eeeee ffffff",
            "ccc a", "dddd bb", "eeeee ccc", "ffffff dddd");

    // 4-gram
    assertTokens(new BothEndsAnalyzer(4, true), "a bb ccc dddd eeeee ffffff",
            "dddd a", "eeeee bb", "ffffff ccc");
  }

  static final class BothEndsAnalyzer extends Analyzer {

    final int size;
    final boolean reverse;

    BothEndsAnalyzer(int size){
      this(size, false);
    }

    BothEndsAnalyzer(int size, boolean reverse){
      this.size = size;
      this.reverse = reverse;
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(final String fieldName){
      final Tokenizer src = new WhitespaceTokenizer();
      ShingleFilter sf = new ShingleFilter(src, size, size);
      sf.setOutputUnigrams(false);
      TokenStream stream = new BothEndsFilter(sf, reverse);
      return new TokenStreamComponents(src, stream);
    }
  }
}
