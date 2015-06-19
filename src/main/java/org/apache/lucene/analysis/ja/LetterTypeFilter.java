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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

import java.io.IOException;

public class LetterTypeFilter extends FilteringTokenFilter {

  private final CharArraySet acceptLetterTypes;
  private final LetterTypeAttribute ltAtt = addAttribute(LetterTypeAttribute.class);

  public LetterTypeFilter(TokenStream in, CharArraySet acceptLetterTypes){
    super(in);
    this.acceptLetterTypes = acceptLetterTypes;
  }

  @Override
  protected boolean accept() throws IOException {
    return acceptLetterTypes.contains(ltAtt.getLetterType());
  }
}
