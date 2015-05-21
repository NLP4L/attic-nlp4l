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

public class BothEndsFilter extends TokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final String tokenSeparator;
  private final boolean reverse;
  public static final String TOKEN_SEPARATOR = " ";

  public BothEndsFilter(TokenStream in){
    this(in, TOKEN_SEPARATOR);
  }

  public BothEndsFilter(TokenStream in, String tokenSeparator){
    this(in, tokenSeparator, false);
  }

  public BothEndsFilter(TokenStream in, boolean reverse){
    this(in, TOKEN_SEPARATOR, reverse);
  }

  public BothEndsFilter(TokenStream in, String tokenSeparator, boolean reverse){
    super(in);
    this.tokenSeparator = tokenSeparator;
    this.reverse = reverse;
  }

  @Override
  public final boolean incrementToken() throws java.io.IOException {
    while (input.incrementToken()){
      final String[] tokens = new String(termAtt.buffer(), 0, termAtt.length()).split(tokenSeparator);
      if(tokens.length >= 2){
        if(reverse)
          termAtt.setEmpty().append(tokens[tokens.length - 1]).append(tokenSeparator).append(tokens[0]);
        else
          termAtt.setEmpty().append(tokens[0]).append(tokenSeparator).append(tokens[tokens.length - 1]);
        return true;
      }
    }

    return false;
  }
}
