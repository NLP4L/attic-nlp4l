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

package org.apache.lucene.analysis.ja;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * 品詞パターンシーケンスから複合語を作る{@link CompoundPOSWordFilter}のファクトリクラス。
 * 汎用的に作られているため、その分処理がやや重いので注意（livedoor コーパスのインデクシング処理の実測値では、
 * 複合語処理を入れると、入れなかったときの3倍時間がかかった）。処理速度が必要な場合は、{@link CompoundNounFilterFactory}が推奨。
 *
 * @rh.solr.schema
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_ja_norm" class="solr.TextField" positionIncrementGap="100" autoGeneratePhraseQueries="true"&gt;
 *   &lt;analyzer&gt;
 *     &lt;charFilter class="solr.MappingCharFilterFactory" mapping="mapping-symbols.txt"/&gt;
 *     &lt;tokenizer class="com.rondhuit.nlp.lucene.JaNBestTokenizerFactory" dir="${dicDir}/juman" type="juman" n="1"/&gt;
 *     &lt;filter class="com.rondhuit.nlp.lucene.JaCompoundPOSWordFilterFactory" compounds="compounds-juman.txt" delimiter=""/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 * 
 */
public class CompoundPOSWordFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
  
  private final String compounds;
  private final String delimiter;
  private final Map<String, CompoundPOSWordFilter.PatCharPair> posCharMap;
  private final List<CompoundPOSWordFilter.PosPatternEq> ppeList;

  public CompoundPOSWordFilterFactory(Map<String, String> args) {
    super(args);
    compounds = require(args, "compounds");
    delimiter = get(args, "delimiter");
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
    posCharMap = new LinkedHashMap<String, CompoundPOSWordFilter.PatCharPair>();   // use LinkedHashMap rather than HashMap for sequencial access
    ppeList = new ArrayList<CompoundPOSWordFilter.PosPatternEq>();
  }

  public void inform(ResourceLoader loader) throws IOException {
    posCharMap.clear();
    ppeList.clear();
    
    InputStream is = loader.openResource(compounds);
    InputStreamReader isr = null;
    BufferedReader br = null;
    
    try{
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      for(String line = br.readLine(); line != null; line = br.readLine()){
        line = line.trim();
        if(line.length() == 0 || line.startsWith("#")) continue;
        ppeList.add(analyzePosPatternEquation(line, posCharMap));
      }
    }
    finally{
      IOUtils.closeQuietly(br);
      IOUtils.closeQuietly(isr);
      IOUtils.closeQuietly(is);
    }
  }
  
  public static final String PATCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  public static final String EQ_SYMBOL = ":=";
  
  static CompoundPOSWordFilter.PosPatternEq analyzePosPatternEquation(String eqLine, Map<String, CompoundPOSWordFilter.PatCharPair> posCharMap){
    if(eqLine.indexOf(EQ_SYMBOL) <= 0){
      throw new IllegalArgumentException(String.format("invalid equation \"%s\"", eqLine));
    }
    
    String[] eqs = eqLine.split(EQ_SYMBOL);
    String rightPatternStr = getPosPatternStr(eqs[1].trim(), posCharMap);
    return new CompoundPOSWordFilter.PosPatternEq(eqs[0].trim(), rightPatternStr);
  }
  
  static String getPosPatternStr(String rightStr, Map<String, CompoundPOSWordFilter.PatCharPair> posCharMap){
    String[] params = rightStr.split("\\s+");
    StringBuilder sb = new StringBuilder();
    for(String param : params){
      String patStr = param.substring(1, param.lastIndexOf(')'));
      CompoundPOSWordFilter.PatCharPair pcPair = posCharMap.get(patStr);
      if(pcPair == null){
        Pattern pat = Pattern.compile(patStr);
        posCharMap.put(patStr, new CompoundPOSWordFilter.PatCharPair(pat, PATCHARS.charAt(posCharMap.size())));
        pcPair = posCharMap.get(patStr);
      }
      sb.append(pcPair.c);
      char eopCh = param.charAt(param.length() - 1);
      if(eopCh == '+' || eopCh == '*' || eopCh == '?')
        sb.append(eopCh);
    }
    
    return sb.toString();
  }

  public TokenStream create(TokenStream input) {
    return new CompoundPOSWordFilter(input, delimiter, posCharMap, ppeList);
  }
}
