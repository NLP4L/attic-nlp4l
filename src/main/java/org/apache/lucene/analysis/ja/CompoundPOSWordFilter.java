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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * 品詞パターンシーケンスから複合語を作る{@link TokenFilter}。
 */
public final class CompoundPOSWordFilter extends TokenFilter {

  public static final char NOT_MATCH_PAT = '@';

  final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
  final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);
  final ReadingAttribute readAtt = addAttribute(ReadingAttribute.class);
  final CompoundPOSAttribute cposAtt = addAttribute(CompoundPOSAttribute.class);
  final CompoundReadingAttribute creadAtt = addAttribute(CompoundReadingAttribute.class);
  final CompoundPronunciationAttribute cproAtt = addAttribute(CompoundPronunciationAttribute.class);

  private List<AttributeSource.State> cache = null;
  private AttributeSource.State finalState;
  private int count;
  final String delimiter;
  final Map<String, PatCharPair> posCharMap;
  final List<PosPatternEq> ppeList;
  private StringBuilder posSequence, compoundTerm, compoundReading, compoundPronunciation;
  private MatchState ms;

  protected CompoundPOSWordFilter(TokenStream input, String delimiter, Map<String, PatCharPair> posCharMap, List<PosPatternEq> ppeList) {
    super(input);
    this.delimiter = delimiter == null ? "" : delimiter;
    this.posCharMap = posCharMap;
    this.ppeList = ppeList;
    posSequence = new StringBuilder();
    compoundTerm = new StringBuilder();
    compoundReading = new StringBuilder();
    compoundPronunciation = new StringBuilder();
    ms = new MatchState();
  }

  public boolean incrementToken() throws IOException {
    if (cache == null) {
      // fill cache lazily
      cache = new ArrayList<AttributeSource.State>();
      fillCache();
      count = 0;
    }

    if (count >= cache.size()) {
      // the cache is exhausted, return false
      return false;
    }
    // Since the TokenFilter can be reset, the tokens need to be preserved as immutable.
    restoreNextState();
    return true;
  }

  @Override
  public final void end() {
    if (finalState != null) {
      restoreState(finalState);
    }
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    cache = null;
    posSequence.setLength(0);
    ms.reset();
  }

  private void fillCache() throws IOException {
    while(input.incrementToken()) {
      cache.add(captureState());
      posSequence.append(getPosPatternChar(posAtt.getPartOfSpeech(), posCharMap));
    }
    // capture final state
    input.end();
    finalState = captureState();
  }

  /*
   * linear search in posCharMap
   */
  static char getPosPatternChar(String pos, Map<String, PatCharPair> posCharMap){
    if(pos == null) return NOT_MATCH_PAT;
    for(String posPatternStr : posCharMap.keySet()){
      PatCharPair pcPair = posCharMap.get(posPatternStr);
      if(pcPair.posPattern.matcher(pos).find()){
        return posCharMap.get(posPatternStr).c;
      }
    }
    return NOT_MATCH_PAT;
  }

  void restoreNextState(){
    if(ms.state == MatchState.State.NOT_YET_MATCH){
      ms = findLeftMatch(ms, ppeList, posSequence, count);
    }

    if(ms.state == MatchState.State.NO_MORE_MATCH){
      restoreState(cache.get(count++));
      return;
    }

    // ok, match found
    if(count < ms.start){
      restoreState(cache.get(count++));
    }
    else{    // count == ms.start
      State compoundState = cache.get(ms.start).clone();
      compoundTerm.setLength(0);
      compoundReading.setLength(0);
      compoundPronunciation.setLength(0);
      int so = 0, eo = 0;
      for(int i = ms.start; i < ms.end; i++){
        restoreState(cache.get(i));
        if(compoundTerm.length() > 0){
          compoundTerm.append(delimiter);
          compoundReading.append(delimiter);
          compoundPronunciation.append(delimiter);
        }
        compoundTerm.append(termAtt.buffer(), 0, termAtt.length());
        compoundReading.append(readAtt.getReading());
        compoundPronunciation.append(readAtt.getPronunciation());
        if(i == ms.start){
          so = offsetAtt.startOffset();
        }
        else if(i == ms.end - 1){
          eo = offsetAtt.endOffset();
        }
      }

      count = ms.end;        // skip count to the end of matcher
      ms.reset();
      restoreState(compoundState);

      termAtt.setEmpty();
      termAtt.append(compoundTerm);
      offsetAtt.setOffset(so, eo);
      posIncAtt.setPositionIncrement(1);
      cposAtt.setCompoundPOS(ms.replacementPosName);
      creadAtt.setCompoundReading(compoundReading.toString());
      cproAtt.setCompoundPronunciation(compoundPronunciation.toString());
    }
  }

  static MatchState findLeftMatch(MatchState ms, List<PosPatternEq> ppeList, StringBuilder posSequence, int count){
    int mostLeftMatchedPos = Integer.MAX_VALUE;
    ms.state = MatchState.State.NO_MORE_MATCH;
    for(PosPatternEq ppe : ppeList){
      Matcher m = ppe.posPattern.matcher(posSequence.substring(count));
      if(m.find()){
        int start = m.start();
        if(mostLeftMatchedPos <= start) continue;
        mostLeftMatchedPos = start;
        ms.setMatch(count + start, count + m.end(), ppe.replacementPosName);
        if(start == 0) break;
      }
    }

    return ms;
  }


  static class PatCharPair {
    final Pattern posPattern;
    final char c;

    public PatCharPair(Pattern posPattern, char c) {
      this.posPattern = posPattern;
      this.c = c;
    }
  }

  static class PosPatternEq {
    final String replacementPosName;
    final Pattern posPattern;

    public PosPatternEq(String replacementPosName, String posPatStr) {
      this.replacementPosName = replacementPosName;
      this.posPattern = Pattern.compile(posPatStr);
    }
  }

  static class MatchState {

    State state;
    int start, end;
    String replacementPosName;

    public MatchState() {
      state = State.NOT_YET_MATCH;
    }

    void setMatch(int start, int end, String replacementPosName){
      this.state = State.MATCH_FOUND;
      this.start = start;
      this.end = end;
      this.replacementPosName = replacementPosName;
    }

    void reset(){
      state = State.NOT_YET_MATCH;
    }

    static enum State {
      NOT_YET_MATCH, MATCH_FOUND, NO_MORE_MATCH;
    }
  }
}
