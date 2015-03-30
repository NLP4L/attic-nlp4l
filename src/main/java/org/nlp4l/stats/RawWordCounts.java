package org.nlp4l.stats;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;

public class RawWordCounts {

  public static long countPrefix(IndexReader reader, String field, String prefix) throws IOException {
    long count = 0;
    Terms terms = MultiFields.getTerms(reader, field);
    TermsEnum te = terms.iterator(null);
    final BytesRef text = new BytesRef(prefix);
    TermsEnum.SeekStatus status = te.seekCeil(text);
    if(status != TermsEnum.SeekStatus.END){
      BytesRef other = te.term();
      while(prefixOf(text, other)){
        count++;
        other = te.next();
      }
    }
    return count;
  }

  static boolean prefixOf(BytesRef target, BytesRef other){
    if(other != null){
      int ol = other.length;
      int tl = target.length;
      if(ol >= tl){
        int oo = other.offset;
        int to = target.offset;
        for(int i = 0; i < tl; i++){
          if(target.bytes[to + i] != other.bytes[oo + i]) return false;
        }
        return true;
      }
      return false;
    }
    return false;
  }
}
