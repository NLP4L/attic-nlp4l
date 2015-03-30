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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

public class BrownCorpusFilter extends TokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  private final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);

  public BrownCorpusFilter(TokenStream in){
    super(in);
  }

  @Override
  public final boolean incrementToken() throws java.io.IOException {
    while (input.incrementToken()){
      int li = search();
      if(li >= 0){
        posAtt.setPartOfSpeech(new String(termAtt.buffer(), li + 1, termAtt.length() - li - 1));
        termAtt.setLength(li);
        int so = offsetAtt.startOffset();
        offsetAtt.setOffset(so, so + li);
      }
      return true;
    }

    return false;
  }

  private int search(){
    char[] buf = termAtt.buffer();
    for(int i = termAtt.length() - 1; i >= 0; i--){
      if(buf[i] == '/') return i;
    }
    return -1;
  }
}