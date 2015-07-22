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

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * {@link CompoundNounFilter}のファクトリクラス。
 * @since 0.1
 * 
 * @rh.solr.schema
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_ja_norm" class="solr.TextField" positionIncrementGap="100" autoGeneratePhraseQueries="true"&gt;
 *   &lt;analyzer&gt;
 *     &lt;charFilter class="solr.MappingCharFilterFactory" mapping="mapping-symbols.txt"/&gt;
 *     &lt;tokenizer class="com.rondhuit.nlp.lucene.JaNBestTokenizerFactory"
 *                   dir="${dicDir}/${dicType}" type="${dicType}" n="3" useSynonyms="true"/&gt;
 *     &lt;filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt" enablePositionIncrements="true"/&gt;
 *     &lt;filter class="com.rondhuit.nlp.lucene.JaCompoundNounFilterFactory" type="${dicType}" delimiter="" unknown="false"/&gt;
 *     &lt;filter class="com.rondhuit.nlp.lucene.JaPOSStopFilterFactory"
 *             tags="stopposs-${dicType}.txt" 
 *             enablePositionIncrements="true"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public final class CompoundNounFilterFactory extends TokenFilterFactory {

  private final String delimiter;
  
  public CompoundNounFilterFactory(Map<String, String> args) {
    super(args);
    delimiter = get(args, "delimiter");
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }
  
  public TokenStream create(TokenStream input) {
    return delimiter == null ?
        new CompoundNounFilter(input) : new CompoundNounFilter(input, delimiter);
  }
}
