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
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

public class LetterTypeFilterFactory extends TokenFilterFactory {

  private final CharArraySet charArraySet = new CharArraySet(1, true);

  protected LetterTypeFilterFactory(Map<String, String> args) {
    super(args);
    String[] letterTypes = require(args, "letterTypes").split(",");
    for(String letterType : letterTypes){
      charArraySet.add(letterType.trim());
    }
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }

  @Override
  public TokenStream create(TokenStream input) {
    return new LetterTypeFilter(input, charArraySet);
  }
}
