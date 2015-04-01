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

package org.apache.lucene.analysis.brown;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class BrownPartOfSpeechFilterTest {

  static class BrownCorpusPOSAnalyzer extends Analyzer {
    private final Set<String> accepted;

    public BrownCorpusPOSAnalyzer(Set<String> accepted) {
      this.accepted = accepted;
    }

    @Override
    protected Analyzer.TokenStreamComponents createComponents(String s) {
      final Tokenizer src = new WhitespaceTokenizer();
      TokenStream stream = new BrownCorpusFilter(src);
      stream = new LowerCaseFilter(stream);
      stream = new BrownPartOfSpeechFilter(stream, accepted);
      return new TokenStreamComponents(src, stream);
    }
  }

  @Test
  public void testFilterNN() throws IOException {
    Set<String> accepted = new HashSet<>(Arrays.asList(new String[]{"nn"}));
    Analyzer analyzer = new BrownCorpusPOSAnalyzer(accepted);

    //             012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567
    String text = "The/at Fulton/np-tl County/nn-tl Grand/jj-tl Jury/nn-tl said/vbd Friday/nr an/at investigation/nn of/in Atlanta's/np$ recent/jj primary/nn election/nn produced/vbd ``/`` no/at evidence/nn ''/'' ./.";
    String[] expectedTerms = new String[]{"investigation", "primary", "election", "evidence"};
    String[] expectedPos = new String[]{"nn","nn","nn","nn"};
    TokenStream stream = analyzer.tokenStream("", text);
    stream.reset();

    CharTermAttribute charAtt = stream.getAttribute(CharTermAttribute.class);
    PartOfSpeechAttribute posAtt = stream.getAttribute(PartOfSpeechAttribute.class);
    int i = 0;
    while(stream.incrementToken()) {
      assertEquals(expectedTerms[i], charAtt.toString());
      assertEquals(expectedPos[i], posAtt.getPartOfSpeech());
      i += 1;
    }
    assertEquals(expectedTerms.length, i);
  }

}
