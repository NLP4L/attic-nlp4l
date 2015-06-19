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

package org.apache.lucene.analysis.ja;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.util.CharArraySet;

public final class NeighboringAnalyzer extends Analyzer {

  private final int within;
  private final CharArraySet charArraySet = new CharArraySet(1, true);

  public NeighboringAnalyzer(){
    this(4, "KATAKANA", "ALPHABET");
  }

  public NeighboringAnalyzer(int within, String... letterTypes){
    this.within = within;
    for(String letterType : letterTypes){
      charArraySet.add(letterType.trim());
    }
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    final Tokenizer tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.NORMAL);
    final TokenStream tokenStream = new LetterTypeFilter(
            new TagLetterTypeFilter(
                    new LowerCaseFilter(
                            new LengthFilter(tokenizer, 2, 100))), charArraySet);
    return new TokenStreamComponents(tokenizer, new NeighboringFilter(tokenStream, within));
  }
}
