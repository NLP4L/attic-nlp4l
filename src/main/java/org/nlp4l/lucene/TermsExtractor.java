/*
 * Copyright 2016 org.NLP4L
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
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 * Luceneインデックスから専門用語を抽出するツール。
 * <br/><br/>
 * プロパティの一覧
<table border="1" cellpadding="3" cellspacing="0">
<tr class="TableHeadingColor">
<th>プロパティ名</th><th>説明</th><th>デフォルト値／必須</th>
</tr>
<tr>
<td>nlp4l.terms.extractor.lucene.index</td><td>Luceneインデックスディレクトリ名</td><td>必須</td>
</tr>
<tr>
<td>nlp4l.terms.extractor.lucene.field.cn</td><td>複合名詞がインデックスされているLuceneフィールド名</td><td>必須</td>
</tr>
<tr>
<td>nlp4l.terms.extractor.lucene.field.ln2</td><td>単名詞左方バイグラムがインデックスされているLuceneフィールド名</td><td>${nlp4l.terms.extractor.lucene.field.cn}_ln2</td>
</tr>
<tr>
<td>nlp4l.terms.extractor.lucene.field.rn2</td><td>単名詞右方バイグラムがインデックスされているLuceneフィールド名</td><td>${nlp4l.terms.extractor.lucene.field.cn}_rn2</td>
</tr>
<tr>
<td>nlp4l.terms.extractor.lucene.delimiter</td><td>複合名詞内のデリミタ文字</td><td>"/"</td>
</tr>
<tr>
<td>nlp4l.terms.extractor.out.file</td><td>抽出した専門用語リストの出力先ファイル名</td><td>標準出力</td>
</tr>
<tr>
<td>nlp4l.terms.extractor.out.score</td><td>抽出した専門用語リストとともにスコアを出力するか否か</td><td>true</td>
</tr>
<tr>
<td>nlp4l.terms.extractor.out.num</td><td>出力する専門用語リスト件数</td><td>1000</td>
</tr>
<tr>
<td>nlp4l.terms.extractor.verbose</td><td>verboseモード</td><td>false</td>
</tr>
</table>
 * 
 * @since 0.3
 */
public class TermsExtractor {

  public static final String DEF_DELIMITER = "/";
  public static final int DEF_OUT_NUM = 1000;

  public static class Config {
    public String scorer;
    public String index;
    public String fieldCn;
    public String fieldLn2;
    public String fieldRn2;
    public String delimiter;
    public String outFile;
    public boolean outScore = true;
    public int outNum = DEF_OUT_NUM;

    String getScorer(){
      if(scorer == null) throw new IllegalArgumentException("scorer must be specified");
      else if(scorer.equals("FreqDFLR")) return "org.nlp4l.lucene.ConcatFreqDFLRCompoundNounScorer";
      else if(scorer.equals("FreqLR")) return "org.nlp4l.lucene.ConcatFreqLRCompoundNounScorer";
      else if(scorer.equals("TypeCountDFLR")) return "org.nlp4l.lucene.ConcatTypeCountDFLRCompoundNounScorer";
      else if(scorer.equals("TypeCountLR")) return "org.nlp4l.lucene.ConcatTypeCountLRCompoundNounScorer";
      else throw new IllegalArgumentException(String.format("invalid scorer is specified (%s)", scorer));
    }

    String getIndex(){
      if(index == null) throw new IllegalArgumentException("index must be specified");
      return index;
    }

    String getFieldNameCn(){
      if(fieldCn == null) throw new IllegalArgumentException("fieldCn must be specified");
      return fieldCn;
    }

    String getFieldNameLn2(){
      if(fieldLn2 != null) return fieldLn2;
      else if(fieldCn != null) return fieldCn + "_ln2";
      throw new IllegalArgumentException("fieldLn2 must be specified");
    }

    String getFieldNameRn2(){
      if(fieldRn2 != null) return fieldRn2;
      else if(fieldCn != null) return fieldCn + "_rn2";
      throw new IllegalArgumentException("fieldRn2 must be specified");
    }

    String getDelimiter(){
      return delimiter == null ? DEF_DELIMITER : delimiter;
    }

    String getOutFile(){
      return outFile;
    }

    boolean getOutScore(){
      return outScore;
    }

    int getOutNum(){
      return outNum;
    }
  }

  Config config;

  private CompoundNounScorer scorer;
  private IndexReader reader;
  private String fieldNameCn, fieldNameLn2, fieldNameRn2, delimiter, outFile;
  private PrintWriter pw;
  private boolean outScore;
  private int outNum;

  private static final Pattern P_SPACES = Pattern.compile("[\\s\\u3000\\u00a0]+");

  public static TermsExtractor getExtractor(Config config){
    TermsExtractor te = new TermsExtractor(config);
    te.setConfig();
    return te;
  }

  protected TermsExtractor(Config config){
    this.config = config;
  }
  
  public void setConfig(){
    fieldNameCn = config.getFieldNameCn();
    fieldNameLn2 = config.getFieldNameLn2();
    fieldNameRn2 = config.getFieldNameRn2();
    delimiter = config.getDelimiter();
    outFile = config.getOutFile();
    outScore = config.getOutScore();
    outNum = config.getOutNum();
  }
  
  void init() throws IOException {
    Directory dir = FSDirectory.open(FileSystems.getDefault().getPath(config.getIndex()));
    reader = DirectoryReader.open(dir);
    pw = outFile == null ? new PrintWriter(System.out) : new PrintWriter(outFile, "UTF-8");

    try {
      // load CompoundNounScorer class
      Class<?> aClass = Class.forName(config.getScorer());
      Constructor<?> aConstr = aClass.getConstructor(IndexReader.class, String.class, String.class, String.class, String.class);
      scorer = (CompoundNounScorer)aConstr.newInstance(reader, delimiter, fieldNameCn, fieldNameLn2, fieldNameRn2);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void execute() throws IOException {
    try{
      init();
      Terms terms = MultiFields.getTerms(reader, fieldNameCn);
      TermsEnum te = terms.iterator();
      BytesRef text = null;
      LuceneDocTermVector.TermWeightQueue queue = new LuceneDocTermVector.TermWeightQueue(outNum);
      
      int count = 0;
      while((text = te.next()) != null){
/*
        if(count % 5000 == 0){
          logger.printTime(count);
        }
*/
        final String term = text.utf8ToString();
        // http://rondhuit-dev.com/trac/projects/ticket/184
        if(P_SPACES.matcher(term).find()) continue;
        final LuceneDocTermVector.TermWeight termWeight = new TermScore((float)scorer.score(term));

        Map.Entry<String,LuceneDocTermVector.TermWeight> entry = new Map.Entry<String,LuceneDocTermVector.TermWeight>() {
          public String getKey() {
            return term;
          }
          public LuceneDocTermVector.TermWeight getValue() {
            return termWeight;
          }
          public LuceneDocTermVector.TermWeight setValue(LuceneDocTermVector.TermWeight arg0) {
            // TODO Auto-generated method stub
            return null;
          }
        };
        queue.insertWithOverflow(entry);
        count++;
      }
      //logger.log("number of compound nouns is %d\n", count);
      printQueue(queue);
    }
    finally{
      try {
        if(reader != null)
          reader.close();
      } catch (IOException e) {
      }
      IOUtils.closeQuietly(pw);
    }
  }
  
  void printQueue(LuceneDocTermVector.TermWeightQueue queue){
    List<Map.Entry<String, LuceneDocTermVector.TermWeight>> list = new ArrayList<Entry<String,LuceneDocTermVector.TermWeight>>(queue.size());
    Map.Entry<String, LuceneDocTermVector.TermWeight> entry = null;
    while((entry = queue.pop()) != null){
      list.add(entry);
    }

    for(int i = list.size() - 1; i >= 0; i--){
      Map.Entry<String, LuceneDocTermVector.TermWeight> e = list.get(i);
      printResultEntry(e);
    }
  }

  protected void printResultEntry(Map.Entry<String, LuceneDocTermVector.TermWeight> e){
    if(outScore)
      pw.printf("%s, %f\n", getTerm(e), getScore(e));
    else
      pw.printf("%s\n", getTerm(e));
  }

  protected String getTerm(Map.Entry<String, LuceneDocTermVector.TermWeight> e){
    return e.getKey().replace(delimiter, "");
  }

  protected float getScore(Map.Entry<String, LuceneDocTermVector.TermWeight> e){
    return e.getValue().weight();
  }

  /**
   * 別途計算したスコアを特定単語の重みとして保持する{@link org.nlp4l.lucene.LuceneDocTermVector.TermWeight}実装クラス。
   *
   * @since 0.3
   */
  public static class TermScore implements LuceneDocTermVector.TermWeight {

    private float score;
    
    public TermScore(float score){
      this.score = score;
    }
    
    public float weight() {
      return score;
    }
  }

  /**
   * 複合名詞のスコア計算を行う抽象クラス。
   * @since 0.3
   */
  public static abstract class CompoundNounScorer {

    protected final IndexReader reader;
    protected final String delimiter, fieldNameCn, fieldNameLn2, fieldNameRn2;

    public CompoundNounScorer(IndexReader reader, String delimiter, String fieldNameCn, String fieldNameLn2, String fieldNameRn2){
      this.reader = reader;
      this.delimiter = delimiter;
      this.fieldNameCn = fieldNameCn;
      this.fieldNameLn2 = fieldNameLn2;
      this.fieldNameRn2 = fieldNameRn2;
    }
    
    public abstract double score(String compNoun) throws IOException;
  }
}
