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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.analysis.miscellaneous.LengthFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.util.FilesystemResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

public class CompoundNounRn2Analyzer extends Analyzer {

  private final UserDictionary userdic;
  ResourceLoader loader = new FilesystemResourceLoader(FileSystems.getDefault().getPath("."));

  public CompoundNounRn2Analyzer(){
    this(null);
  }

  public CompoundNounRn2Analyzer(String dic){
    if(dic != null){
      try {
        userdic = UserDictionary.open(new FileReader(dic));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    else{
      userdic = null;
    }
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    final Tokenizer tokenizer = new JapaneseTokenizer(userdic, true, JapaneseTokenizer.Mode.NORMAL);
    StopFilterFactory sff = new StopFilterFactory(mapArg("words", "examples/schema/stopwords.txt"));
    JapanesePartOfSpeechStopFilterFactory jpossff = new JapanesePartOfSpeechStopFilterFactory(mapArg("tags", "examples/schema/stopposs.txt"));
    PatternReplaceFilterFactory prff = new PatternReplaceFilterFactory(mapArg("pattern", "^\\d+.*", "replacement", "RemoveMe"));
    StopFilterFactory sff2 = new StopFilterFactory(mapArg("words", "examples/schema/stopwords-2.txt"));
    ShingleFilterFactory shff = new ShingleFilterFactory(
            mapArg("minShingleSize", "2", "maxShingleSize", "2",
                    "outputUnigrams", "false", "outputUnigramsIfNoShingles", "false",
                    "tokenSeparator", "/"));
    PatternReplaceFilterFactory prff2 = new PatternReplaceFilterFactory(mapArg("pattern", "^_/.+", "replacement", "", "replace", "all"));
    PatternReplaceFilterFactory prff3 = new PatternReplaceFilterFactory(mapArg("pattern", ".+/_$", "replacement", "", "replace", "all"));
    LengthFilterFactory lff = new LengthFilterFactory(mapArg("min", "1", "max", "100"));
    try {
      sff.inform(loader);
      jpossff.inform(loader);
      sff2.inform(loader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    final TokenStream tokenStream = lff.create(
            prff3.create(
                    prff2.create(
                            shff.create(
                                    sff2.create(
                                            prff.create(
                                                    jpossff.create(
                                                            sff.create(tokenizer)
                                                    )
                                            )
                                    )
                            )
                    )
            )
    );
    return new TokenStreamComponents(tokenizer, tokenStream);
  }

  static Map<String, String> mapArg(String... args){
    if((args.length % 2) == 1) throw new IllegalArgumentException("number of arguments should be even");
    if(args == null) return new HashMap<>();

    Map<String, String> map = new HashMap<>(args.length);
    for(int i = 0; i < args.length; i += 2){
      map.put(args[i], args[i + 1]);
    }
    return map;
  }
}

/*
<fieldType name="text_ja_rn2" class="solr.TextField" positionIncrementGap="100" autoGeneratePhraseQueries="true">
  <analyzer>
	<charFilter class="solr.MappingCharFilterFactory" mapping="mapping-symbols.txt"/>
	<tokenizer class="com.rondhuit.nlp.lucene.JaNBestTokenizerFactory" n="1"
                   dir="${dicDir}/${dicType}" type="${dicType}" useSynonyms="false"/>
	<filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt" enablePositionIncrements="true"/>
	<filter class="com.rondhuit.nlp.lucene.JaPOSStopFilterFactory" tags="stopposs-${dicType}.txt"/>
	<filter class="solr.PatternReplaceFilterFactory" pattern="^\d+.*" replacement="RemoveMe"/>
	<filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords-2.txt" enablePositionIncrements="true"/>
	<filter class="solr.ShingleFilterFactory" minShingleSize="2" maxShingleSize="2"
		outputUnigrams="false" outputUnigramsIfNoShingles="false" tokenSeparator="/"/>

	<filter class="solr.PatternReplaceFilterFactory" pattern="^_/.+" replacement="" replace="all"/>
	<filter class="solr.PatternReplaceFilterFactory" pattern=".+/_$" replacement="" replace="all"/>
	<filter class="solr.LengthFilterFactory" min="1" max="1000" enablePositionIncrements="true"/>
  </analyzer>
</fieldType>
 */
