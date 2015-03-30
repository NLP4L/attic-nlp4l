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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.junit.Test;

import java.io.IOException;
import static org.junit.Assert.*;

public class BrownCorpusAnalyzerTest {
  
  @Test
  public void testNoPOSBrownCorpusFilter() throws IOException {
    BrownCorpusAnalyzer analyzer = new BrownCorpusAnalyzer();
    String text = "The Fulton County Grand Jury said Friday an/at investigation of/in Atlanta's/np$ recent primary election produced `` no evidence '' .";
    String[] expectedTerms = new String[]{"the", "fulton", "county", "grand", "jury", "said", "friday", "an", "investigation", "of", "atlanta's", "recent", "primary", "election", "produced", "``", "no", "evidence", "''", "."};
    String[] expectedPos = new String[]{null,null,null,null,null,null,null,"at",null,"in","np$",null,null,null,null,null,null,null,null,null};
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
  
  @Test
  public void testSimpleFullPOSBrownCorpusFilter() throws IOException {
    BrownCorpusAnalyzer analyzer = new BrownCorpusAnalyzer();
    //                       1         2         3         4         5         6         7         8         9         0         1         2         3         4         5         6         7         8         9
    //             012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567
    String text = "The/at Fulton/np-tl County/nn-tl Grand/jj-tl Jury/nn-tl said/vbd Friday/nr an/at investigation/nn of/in Atlanta's/np$ recent/jj primary/nn election/nn produced/vbd ``/`` no/at evidence/nn ''/'' ./.";
    String[] expectedTerms = new String[]{"the", "fulton", "county", "grand", "jury", "said", "friday", "an", "investigation", "of", "atlanta's", "recent", "primary", "election", "produced", "``", "no", "evidence", "''", "."};
    String[] expectedPos = new String[]{"at","np-tl","nn-tl","jj-tl","nn-tl","vbd","nr","at","nn","in","np$","jj","nn","nn","vbd","``","at","nn","''","."};
    int[] expectedEOffsets = new int[]{3,13,26,38,49,60,71,77,94,100,113,124,135,147,159,166,172,184,190,195};
    TokenStream stream = analyzer.tokenStream("", text);
    stream.reset();

    CharTermAttribute charAtt = stream.getAttribute(CharTermAttribute.class);
    PartOfSpeechAttribute posAtt = stream.getAttribute(PartOfSpeechAttribute.class);
    OffsetAttribute offAtt = stream.getAttribute(OffsetAttribute.class);
    int i = 0;
    while(stream.incrementToken()) {
      assertEquals(expectedTerms[i], charAtt.toString());
      assertEquals(expectedPos[i], posAtt.getPartOfSpeech());
      assertEquals(expectedEOffsets[i], offAtt.endOffset());
      i += 1;
    }
    assertEquals(expectedTerms.length, i);
  }
}
