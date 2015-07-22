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
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

/**
 * 連接種類数LR法にしたがい複合名詞CNのスコアを計算する。
 * 
 * @see LRCompoundNounScorer#score(String)
 * @since 0.3
 */
public class ConcatTypeCountLRCompoundNounScorer extends LRCompoundNounScorer {

  public ConcatTypeCountLRCompoundNounScorer(IndexReader reader, String delimiter,
      String fieldNameCn, String fieldNameLn2, String fieldNameRn2) {
    super(reader, delimiter, fieldNameCn, fieldNameLn2, fieldNameRn2);
  }

  protected double getConcatenatedNounScore(String fieldName, String noun)
      throws IOException {
    Terms terms = MultiFields.getTerms(reader, fieldName);
    TermsEnum te = terms.iterator();
    te.seekCeil(new BytesRef(noun));
    BytesRef text = te.term();
    int count = 0;
    do {
      if(text == null || !text.utf8ToString().startsWith(noun)) break;
      count++;
    } while((text = te.next()) != null);
    return count;
  }
}
