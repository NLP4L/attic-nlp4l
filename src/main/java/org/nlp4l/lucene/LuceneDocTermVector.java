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
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;

/**
 * Luceneインデックスから特定文書のタームベクトルを取得するクラス。
 * 
 * @since 0.2
 */
public class LuceneDocTermVector {
  
  private final TermWeightQueue queue;

  /**
   * Luceneインデックスから特定文書のタームベクトルを取得する。
   * 
   * @param reader 対象となるLuceneインデックスをオープンしている{@link IndexReader}。
   * @param docId タームベクトルを取得する対象のLucene文書ID。
   * @param fieldName タームベクトルを取得する対象のLuceneフィールド名。
   * @param size タームベクトルのサイズ。
   * @throws IOException
   */
  public LuceneDocTermVector(IndexReader reader, int docId, String fieldName, int size) throws IOException {
    this(reader, docId, fieldName, size, null, null, null, null);
  }

  /**
   * Luceneインデックスから特定文書のタームベクトルを取得する。
   * 
   * @param reader 対象となるLuceneインデックスをオープンしている{@link IndexReader}。
   * @param docId タームベクトルを取得する対象のLucene文書ID。
   * @param fieldName タームベクトルを取得する対象のLuceneフィールド名。
   * @param size タームベクトルのサイズ。
   * @param termsReuse nullの場合は作成される。
   * @param twf nullの場合は{@link DefaultTfIdfTermWeightFactory}が作成される。
   * @throws IOException
   */
  public LuceneDocTermVector(IndexReader reader, int docId, String fieldName, int size,
      Terms termsReuse, TermWeightFactory twf) throws IOException {
    this(reader, docId, fieldName, size, termsReuse, null, twf, null);
  }

  /**
   * Luceneインデックスから特定文書のタームベクトルを取得する。
   * 
   * @param reader 対象となるLuceneインデックスをオープンしている{@link IndexReader}。
   * @param docId タームベクトルを取得する対象のLucene文書ID。
   * @param fieldName タームベクトルを取得する対象のLuceneフィールド名。
   * @param size タームベクトルのサイズ。
   * @param termsReuse nullの場合は作成される。
   * @param liveDocs nullの場合は作成される。
   * @throws IOException
   */
  public LuceneDocTermVector(IndexReader reader, int docId, String fieldName, int size,
      Terms termsReuse, Bits liveDocs) throws IOException {
    this(reader, docId, fieldName, size, termsReuse, liveDocs, null, null);
  }

  /**
   * Luceneインデックスから特定文書のタームベクトルを取得する。
   * 
   * @param reader 対象となるLuceneインデックスをオープンしている{@link IndexReader}。
   * @param docId タームベクトルを取得する対象のLucene文書ID。
   * @param fieldName タームベクトルを取得する対象のLuceneフィールド名。
   * @param size タームベクトルのサイズ。
   * @param termsReuse nullの場合は作成される。
   * @param liveDocs nullの場合は作成される。
   * @param twf nullの場合は{@link DefaultTfIdfTermWeightFactory}が作成される。
   * @param stopWords タームベクトルの要素としたくない単語の集合を指定する。使用しない場合はnullが指定できる。
   * @throws IOException
   */
  public LuceneDocTermVector(IndexReader reader, int docId, String fieldName, int size,
      Terms termsReuse, Bits liveDocs, TermWeightFactory twf, Set<String> stopWords) throws IOException {
    liveDocs = liveDocs == null ? MultiFields.getLiveDocs(reader) : liveDocs;
    twf = twf == null ? new DefaultTfIdfTermWeightFactory(reader, docId, fieldName, liveDocs) : twf;
    queue = new TermWeightQueue(size);
    
    if(termsReuse == null)
      termsReuse = reader.getTermVector(docId, fieldName);
    TermsEnum termsEnum = termsReuse.iterator();
    BytesRef text;
    while((text = termsEnum.next()) != null){
      // candidate feature term
      final String term = text.utf8ToString();
      if(stopWords != null && stopWords.contains(term)) continue;
      final TermWeight termWeight = twf.create(text);
      if(termWeight == null) continue;

      Map.Entry<String,TermWeight> entry = new Map.Entry<String,TermWeight>() {
        public String getKey() {
          return term;
        }
        public TermWeight getValue() {
          return termWeight;
        }
        public TermWeight setValue(TermWeight arg0) {
          // TODO Auto-generated method stub
          return null;
        }
      };
      queue.insertWithOverflow(entry);
    }
  }
  
  public TermWeightQueue getResultQueue(){
    return queue;
  }
  
  /**
   * Luceneインデックス中の特定単語の重みを取得するためのインタフェース。
   * 
   * @since 0.2
   *
   */
  public static interface TermWeight{
    public float weight();
  }
  
  /**
   * Luceneインデックス中の特定単語の重みをtf*idfを用いて計算するデフォルト{@link TermWeight}実装クラス。
   * 
   * @since 0.2
   *
   */
  protected static class DefaultTfIdfTermWeight implements TermWeight {
    
    private final int maxDoc, tf, docFreq;
    
    public DefaultTfIdfTermWeight(int maxDoc, int tf, int docFreq){
      this.maxDoc = maxDoc;
      this.tf = tf;
      this.docFreq = docFreq;
    }

    public float weight() {
      return (float)(Math.sqrt(tf) * (1 + Math.log(maxDoc / (docFreq + 1))));
    }
    
    public int maxDoc(){ return maxDoc; }
    public int tf(){ return tf; }
    public int docFreq(){ return docFreq; }
  }
  
  /**
   * {@link TermWeight}を生成するためのファクトリインタフェース。
   * 
   * @since 0.2
   */
  public static interface TermWeightFactory {
    public TermWeight create(BytesRef term) throws IOException;
  }
  
  /**
   * {@link TermWeight}として{@link DefaultTfIdfTermWeight}を生成する{@link TermWeightFactory}の実装クラス。
   * 
   * @since 0.2
   *
   */
  public static class DefaultTfIdfTermWeightFactory implements TermWeightFactory {
    
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final int maxDoc;
    private final int docId;
    private final String fieldName;
    private final Bits liveDocs;
    
    public DefaultTfIdfTermWeightFactory(IndexReader reader, int docId, String fieldName, Bits liveDocs){
      this.reader = reader;
      searcher = new IndexSearcher(reader);
      maxDoc = reader.maxDoc();
      this.docId = docId;
      this.fieldName = fieldName;
      this.liveDocs = liveDocs == null ? MultiFields.getLiveDocs(reader) : liveDocs;
    }

    public TermWeight create(BytesRef term) throws IOException {
      PostingsEnum docsEnum = MultiFields.getTermDocsEnum(reader, liveDocs, fieldName, term);
      int d = docsEnum.advance(docId);
      if(d != docId){
        throw new RuntimeException("wrong docId!");
      }
      final int tf = docsEnum.freq();
      final int docFreq = docFreq(term);
      return new DefaultTfIdfTermWeight(maxDoc, tf, docFreq);
    }
    
    protected int docFreq(BytesRef text) throws IOException {
      return docFreq(text.utf8ToString());
    }
    
    protected int docFreq(String text) throws IOException {
      return LuceneUtil.getTermDocFreq(searcher, fieldName, text);
    }
  }
  
  final static class TermWeightQueue extends PriorityQueue<Map.Entry<String,TermWeight>> {
    
    public TermWeightQueue(int maxSize) {
      super(maxSize);
    }

    // collect terms with larger weight
    protected boolean lessThan(Entry<String,TermWeight> a, Entry<String,TermWeight> b){
      return a.getValue().weight() < b.getValue().weight();
    }
  }
}
