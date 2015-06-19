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

import java.util.regex.Pattern;

public class TagLetterTypeFilter extends TokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final LetterTypeAttribute ltAtt = addAttribute(LetterTypeAttribute.class);
  public static final Pattern PAT_ALPHABET = Pattern.compile("^[a-zA-Z]+$");
  public static final Pattern PAT_NUMBER = Pattern.compile("^[0-9]+$");
  public static final Pattern PAT_HIRAGANA = Pattern.compile("^[\\u3040-\\u309F]+$");
  public static final Pattern PAT_KATAKANA = Pattern.compile("^[\\u30A0-\\u30FF]+$");

  public TagLetterTypeFilter(TokenStream in){
    super(in);
  }

  @Override
  public final boolean incrementToken() throws java.io.IOException {
    while(input.incrementToken()){
      String term = termAtt.toString();
      if(PAT_ALPHABET.matcher(term).find()) ltAtt.setLetterType("ALPHABET");
      else if(PAT_NUMBER.matcher(term).find()) ltAtt.setLetterType("NUMBER");
      else if(PAT_HIRAGANA.matcher(term).find()) ltAtt.setLetterType("HIRAGANA");
      else if(PAT_KATAKANA.matcher(term).find()) ltAtt.setLetterType("KATAKANA");
      else ltAtt.setLetterType("MISC");
      return true;
    }

    return false;
  }
}
