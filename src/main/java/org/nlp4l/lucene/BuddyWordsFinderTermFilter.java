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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

public abstract class BuddyWordsFinderTermFilter {
  
  protected IndexReader reader;
  protected float numDocs;
  protected final float minTermPopularity, maxTermPopularity;
  protected final int minTermLength, maxTermLength;
  protected TermsEnum currentTE;
  protected int currentTermDocFreq;
  
  public BuddyWordsFinderTermFilter(){
    this.minTermPopularity = 0.0002F;
    this.maxTermPopularity = 0.1F;
    this.minTermLength = 2;
    this.maxTermLength = 64;
  }

  protected boolean skip(BytesRef term) throws IOException {
    return skip(term.utf8ToString());
  }

  protected boolean skip(String term) throws IOException {
    return skipByLength(term);
  }

  protected boolean skipByLength(String term) throws IOException {
    return term.length() < minTermLength || term.length() > maxTermLength;
  }

  protected boolean skipByPopularity(BytesRef term) throws IOException {
    float percent = (float)currentTermDocFreq / numDocs;
    //System.out.printf("%s\t%1.6f\n", term.utf8ToString(), percent);
    if(percent < minTermPopularity || percent > maxTermPopularity)
      return true;
    else
      return false;
  }

  public void close() throws IOException {
  }

  public final void start(IndexReader reader, String field, BytesRef term) throws IOException {
    this.reader = reader;
    numDocs = reader.numDocs();
    // TODO: can we ignore term value?
    currentTE = MultiFields.getTerms(reader, field).iterator();
    if(currentTE != null){
      currentTE.seekCeil(term);
      currentTermDocFreq = currentTE.docFreq();
    }
  }
  
  public int getCurrentTermDocFreq(){
    return currentTermDocFreq;
  }
}
