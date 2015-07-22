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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TotalHitCountCollector;

/**
 * Luceneの共通関数を集めたクラス。
 */
public class LuceneUtil {

  /**
   * 引数で与えられた条件で{@link TermQuery}を作成して検索し、ヒット件数を返す。
   * 
   * @param searcher 検索対象Luceneインデックスをオープンしている{@link IndexSearcher}
   * @param fieldName 検索対象フィールド名
   * @param term 検索単語
   * @return ヒット件数
   * @throws IOException
   */
  public static int getTermDocFreq(IndexSearcher searcher, String fieldName, String term) throws IOException {
    Query query = new TermQuery(new Term(fieldName, term));
    TotalHitCountCollector c = new TotalHitCountCollector();
    searcher.search(query, c);
    return c.getTotalHits();
  }
}
