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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.AttributeSource;

import java.io.IOException;

public final class OffsetsGapFillerFilter extends TokenFilter {

  final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  private int eo = -1;
  private AttributeSource.State loanState;

  protected OffsetsGapFillerFilter(TokenStream input) {
    super(input);
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    eo = -1;
    loanState = null;
  }

  @Override
  public boolean incrementToken() throws IOException {
    // not need to call correctOffset here
    if(loanState != null){
      restoreState(loanState);
      loanState = null;
      eo = offsetAtt.endOffset();
      return true;
    }
    else if(input.incrementToken()){
      if(eo == -1){
        eo = offsetAtt.endOffset();
      }
      else{
        int so = offsetAtt.startOffset();
        if(so > eo){
          // gap between end and start offsets
          loanState = captureState();
          clearAttributes();
          int size = so - eo;
          termAtt.setEmpty().resizeBuffer(size);
          for(int i = 0; i < size; i++){
            termAtt.append(' ');
          }
          offsetAtt.setOffset(eo, so);
        }
        else if(so < eo){
          // this cannot be happening!
          throw new RuntimeException(String.format("start offset (%d) of the term '%s' is smaller than end offset (%d) of the previous term",
                  so, termAtt.toString(), eo));
        }
        else{
          eo = offsetAtt.endOffset();
        }
      }
      return true;
    }
    else {
      return false;
    }
  }
}
