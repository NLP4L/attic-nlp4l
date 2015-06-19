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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;

public class NeighboringFilter extends TokenFilter {

  private final int within;
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final PositionIncrementAttribute posAtt = addAttribute(PositionIncrementAttribute.class);
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  private String prevTerm;
  private int startOffset, endOffset;

  protected NeighboringFilter(TokenStream input, int within) {
    super(input);
    this.within = within;
  }

  @Override
  public boolean incrementToken() throws IOException {
    while(input.incrementToken()){
      if(prevTerm == null){
        prevTerm = new String(termAtt.buffer(), 0, termAtt.length());
        startOffset = offsetAtt.startOffset();
      }
      else{
        int posInc = posAtt.getPositionIncrement();
        if(posInc <= within){
          String nextTerm = new String(termAtt.buffer(), 0, termAtt.length());
          endOffset = offsetAtt.endOffset();
          termAtt.setEmpty().append(sort(prevTerm, nextTerm));
          posAtt.setPositionIncrement(posInc + 1);
          offsetAtt.setOffset(startOffset, endOffset);
          // for the next iteration
          prevTerm = nextTerm;
          startOffset = endOffset;
          return true;
        }
        else{
          prevTerm = null;
        }
      }
    }

    return false;
  }

  private String sort(String s1, String s2){
    return s1.compareTo(s2) < 0 ? s1 + " " + s2 : s2 + " " + s1;
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    prevTerm = null;
  }
}
