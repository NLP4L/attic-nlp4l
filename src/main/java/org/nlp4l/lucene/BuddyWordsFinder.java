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
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PriorityQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @see <a href="https://issues.apache.org/jira/browse/LUCENE-474">LUCENE-474</a>
 *
 */
public class BuddyWordsFinder {

  public static Logger log = LoggerFactory.getLogger(BuddyWordsFinder.class);
  
  private final IndexReader reader;
  private final int maxDocsToAnalyze, slop, maxCoiTermsPerTerm;
  private final BitSet termPos;
  private final Map<String, Scorer> phraseTerms = new HashMap<String, Scorer>();
  private final BuddyWordsFinderTermFilter baseTermFilter, coiTermFilter;

  public BuddyWordsFinder(IndexReader reader,
                           int maxDocsToAnalyze, int slop, int maxCoiTermsPerTerm, int maxBaseTermsPerDoc,
                           BuddyWordsFinderTermFilter baseTermFilter, BuddyWordsFinderTermFilter coiTermFilter){
    this.reader = reader;
    this.maxDocsToAnalyze = maxDocsToAnalyze;
    this.slop = slop;
    this.maxCoiTermsPerTerm = maxCoiTermsPerTerm;
    termPos = new BitSet(maxBaseTermsPerDoc);
    this.baseTermFilter = baseTermFilter;
    this.coiTermFilter = coiTermFilter;
  }

  public Scorer[] findCoincidentalTerms(String field, BytesRef term) throws IOException {

    baseTermFilter.start(reader, field, term);
    if(baseTermFilter.skip(term) ||
        baseTermFilter.skipByPopularity(term))
      return null;
    //System.out.println(term.utf8ToString());

    Bits liveDocs = MultiFields.getLiveDocs(reader);
    
    PostingsEnum de = MultiFields.getTermDocsEnum(reader, field, term);
    if(de == null) return null;
    int numDocsAnalyzed = 0;
    phraseTerms.clear();

    while(de.nextDoc() != PostingsEnum.NO_MORE_DOCS && numDocsAnalyzed < maxDocsToAnalyze){
      int docId = de.docID();

      //first record all of the positions of the term in a bitset which
      // represents terms in the current doc.
      int freq = de.freq();
      PostingsEnum pe = MultiFields.getTermPositionsEnum(reader, field, term);
      int ret = pe.advance(docId);
      if(ret == PostingsEnum.NO_MORE_DOCS) continue;
      termPos.clear();
      for(int i = 0; i < freq; i++){
        int pos = pe.nextPosition();
        if(pos < termPos.size())
          termPos.set(pos);
      }

      // now look at all OTHER terms in this doc and see if they are
      // positioned in a pre-defined sized window around the current term
      Fields vectors = reader.getTermVectors(docId);
      // check it has term vectors
      if(vectors == null) return null;
      Terms vector = vectors.terms(field);
      // check it has position info
      if(vector == null || !vector.hasPositions()) return null;

      TermsEnum te = vector.iterator();
      BytesRef otherTerm = null;
      while((otherTerm = te.next()) != null){
        if(term.bytesEquals(otherTerm)) continue;
        coiTermFilter.start(reader, field, otherTerm);
        if(coiTermFilter.skip(otherTerm) || coiTermFilter.skipByPopularity(otherTerm)) continue;

        PostingsEnum pe2 = MultiFields.getTermPositionsEnum(reader, field, otherTerm);
        ret = pe2.advance(docId);
        if(ret == PostingsEnum.NO_MORE_DOCS) continue;
        freq = pe2.freq();
        boolean matchFound = false;
        for(int i = 0; i < freq && (!matchFound); i++){
          int pos = pe2.nextPosition();
          int startpos = Math.max(0, pos - slop);
          int endpos = pos + slop;
          for (int prevpos = startpos;
            (prevpos <= endpos) && (!matchFound); prevpos++){
            if(termPos.get(prevpos)){
              // Add term to hashmap containing co-occurence
              // counts for this term
              Scorer pt = phraseTerms.get(otherTerm.utf8ToString());
              if(pt == null){
                pt = new Scorer(baseTermFilter.getCurrentTermDocFreq(),
                    otherTerm.utf8ToString(), coiTermFilter.getCurrentTermDocFreq());
                phraseTerms.put(pt.coiTerm, pt);
              }
              pt.incCoiDocCount();
              matchFound = true;
            }
          }
        }
      }
      numDocsAnalyzed++;
    } // end of while loop

    // now sort and dump the top terms associated with this term.
    TopTerms topTerms = new TopTerms(maxCoiTermsPerTerm);
    for(String key : phraseTerms.keySet()){
      Scorer pt = phraseTerms.get(key);
      topTerms.insertWithOverflow(pt);
    }
    Scorer[] tops = new Scorer[topTerms.size()];
    int tp = tops.length - 1;
    while(topTerms.size() > 0){
      Scorer top = topTerms.pop();
      tops[tp--] = top;
    }
    return tops;
  }
  
  public static final class Scorer {
    public final String coiTerm;
    final int baseTermDocFreq;
    final int coiTermDocFreq;
    int coiDocCount;
    public float score = Float.MIN_VALUE;

    public Scorer(int baseTermDocFreq, String coiTerm, int coiTermDocFreq){
      this.baseTermDocFreq = baseTermDocFreq;
      this.coiTerm = coiTerm;
      this.coiTermDocFreq = coiTermDocFreq;
      coiDocCount = 0;
    }
    public void incCoiDocCount(){
      coiDocCount++;
    }

    @Override
    public String toString(){
      return String.format("%s(%d, %d, %d, %f)", coiTerm, baseTermDocFreq, coiTermDocFreq, coiDocCount, score());
    }

    public float score(){
      if(score == Float.MIN_VALUE){
        float overallIntersectionPercent =
                (float)coiDocCount / (float)(baseTermDocFreq + coiTermDocFreq - coiDocCount);
        float coiIntersectionPercent = (float)coiDocCount / (float)coiTermDocFreq;
        score = (overallIntersectionPercent + coiIntersectionPercent) / 2;
      }
      return score;
    }
  }

  public static class TopTerms extends PriorityQueue<Scorer>{
    public TopTerms(int size){
      super(size);
    }
    protected boolean lessThan(Scorer a, Scorer b){
      float sa = a.score();
      float sb = b.score();
      return sa == sb ? (a.coiTerm.compareTo(b.coiTerm) > 0 ? true : false) : a.score() < b.score();
    }
  }
}
