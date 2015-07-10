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

package org.nlp4l.lucene;

import java.io.IOException;
import java.util.Set;

public class StopTermFilter extends BuddyWordsFinderTermFilter {

  final Set<String> stopWords;
  final boolean ignoreCase;
  
  public StopTermFilter(Set<String> stopWords, boolean ignoreCase) {
    this.stopWords = stopWords;
    this.ignoreCase = ignoreCase;
  }

  @Override
  protected boolean skip(String term) throws IOException {
    //if(super.skip(term)) return true;
    //return stopWords.contains(ignoreCase ? term.toLowerCase() : term);
    return super.skip(term);
  }
}
