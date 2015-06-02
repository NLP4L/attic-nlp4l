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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

public class BothEndsFilterFactory extends TokenFilterFactory {

  private final String tokenSeparator;
  private final boolean reverse;

  public BothEndsFilterFactory(Map<String, String> args){
    super(args);
    tokenSeparator = get(args, "tokenSeparator", BothEndsFilter.TOKEN_SEPARATOR);
    reverse = getBoolean(args, "reverse", false);
  }

  @Override
  public TokenStream create(TokenStream stream) {
    return new BothEndsFilter(stream, tokenSeparator, reverse);
  }
}