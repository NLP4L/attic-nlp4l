package org.apache.lucene.analysis.brown;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

public final class BrownCorpusAnalyzer extends Analyzer {

  @Override
  protected Analyzer.TokenStreamComponents createComponents(final String fieldName) {
    final Tokenizer src = new WhitespaceTokenizer();
    TokenStream stream = new BrownCorpusFilter(src);
    stream = new LowerCaseFilter(stream);
    return new TokenStreamComponents(src, stream);
  }
}
