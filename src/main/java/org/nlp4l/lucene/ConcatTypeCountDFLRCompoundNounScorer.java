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

/**
 * 連接種類数FLR法にしたがい複合名詞CNのスコアを計算する。ただし、CNの単独出現数はdocFreqを使用する近似を行っている。
 * くわしくは、{@link #score(String)}を参照のこと。
 * 
 * @see LRCompoundNounScorer#score(String)
 * @since 0.3
 */
public class ConcatTypeCountDFLRCompoundNounScorer extends
    ConcatTypeCountLRCompoundNounScorer {

  public ConcatTypeCountDFLRCompoundNounScorer(IndexReader reader,
      String delimiter, String fieldNameCn, String fieldNameLn2,
      String fieldNameRn2) {
    super(reader, delimiter, fieldNameCn, fieldNameLn2, fieldNameRn2);
  }

  /**
   * 複合名詞CNのスコアを、以下の計算式から計算する。f(CN)はdocFreq(CN)で近似している。<br/><br/>
   * 
   * \begin{align*}
   * FLR(CN) & =f(CN) \times LR(CN) \\
   * & \approx docFreq(CN) \times LR(CN)
   * \end{align*}
   * 
   * <br/><br/>
   * その他の記号については{@link LRCompoundNounScorer#score(String)}を参照のこと。<br/><br/>
   * <ul>
   * <li>複合名詞CNの単独出現数：f(CN)</li>
   * <li>連接種類数LR法{@link ConcatTypeCountLRCompoundNounScorer}によるスコア：LR(CN)</li>
   * <li>複合名詞CNを単独で含む文書数：docFreq(CN)</li>
   * </ul>
   * 
   * @param compNoun 複合名詞CN
   * @see LRCompoundNounScorer#score(String)
   * 
   */
  public double score(String compNoun) throws IOException {
    return LuceneUtil.getTermDocFreq(searcher, fieldNameCn, compNoun) * super.score(compNoun);
  }
}
