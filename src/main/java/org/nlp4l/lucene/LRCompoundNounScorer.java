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
import org.apache.lucene.search.IndexSearcher;

/**
 * 複合名詞CNのスコアを、CNを構成する各単名詞Nの左方／右方スコア関数を計算した上で両者を組み合わせて求める抽象クラス。
 * 詳しい計算式は、{@link #score(String)}を参照のこと。
 * 
 * @since 0.3
 */
public abstract class LRCompoundNounScorer extends TermsExtractor.CompoundNounScorer {
  
  protected IndexSearcher searcher;

  public LRCompoundNounScorer(IndexReader reader, String delimiter, String fieldNameCn,
      String fieldNameLn2, String fieldNameRn2) {
    super(reader, delimiter, fieldNameCn, fieldNameLn2, fieldNameRn2);
    searcher = new IndexSearcher(reader);
  }

  /**
   * 複合名詞CNのスコアを、以下の計算式から計算する。<br/><br/>
   * 
   * \[
   * LR(CN)=\left \{ \prod_{i=1}^{L}(FL(N_{i})+1)(FR(N_{i})+1) \right \}^{\frac{1}{2L}}
   * \]
   * 
   * <ul>
   * <li>単名詞：N<sub>i</sub></li>
   * <li>複合名詞：CN=N<sub>1</sub>N<sub>2</sub>...N<sub>L</sub></li>
   * <li>{@link #getLeftConcatenatedNounScore(String)}により計算する単名詞N<sub>i</sub>の左方スコア関数：FL(N<sub>i</sub>)</li>
   * <li>{@link #getRightConcatenatedNounScore(String)}により計算する単名詞N<sub>i</sub>の右方スコア関数：FR(N<sub>i</sub>)</li>
   * </ul>
   * 
   * @param compNoun 複合名詞CN
   */
  public double score(String compNoun) throws IOException {
    String[] nouns = compNoun.split(delimiter);
    double score = 1;
    for(String noun : nouns){
      score *= (getLeftConcatenatedNounScore(noun) + 1) * (getRightConcatenatedNounScore(noun) + 1);
    }
    return Math.pow(score, 1.0 / (double)(2 * nouns.length));
  }
  
  protected double getLeftConcatenatedNounScore(String noun) throws IOException{
    return getConcatenatedNounScore(fieldNameLn2, noun);
  }
  
  protected double getRightConcatenatedNounScore(String noun) throws IOException{
    return getConcatenatedNounScore(fieldNameRn2, noun);
  }
  
  protected abstract double getConcatenatedNounScore(String fieldName, String noun) throws IOException;
  
  protected int getCompoundNounDocFreq(String compNoun) throws IOException {
    return LuceneUtil.getTermDocFreq(searcher, fieldNameCn, compNoun);
  }
}
