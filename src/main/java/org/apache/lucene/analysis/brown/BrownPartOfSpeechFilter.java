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

import java.io.IOException;
import java.util.Set;

public class BrownPartOfSpeechFilter extends TokenFilter {

  private final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);
  private final Set<String> accepted;

  public BrownPartOfSpeechFilter(TokenStream in, Set<String> accepted) {
    super(in);
    this.accepted = accepted;
  }

  @Override
  public final boolean incrementToken() throws IOException {
    while (input.incrementToken()) {
      String pos = posAtt.getPartOfSpeech();
      if (accepted.contains(pos))
        return true;
    }
    return false;
  }
}
