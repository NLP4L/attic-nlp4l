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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * 連続した名詞を複合名詞として1つにまとめる。
 * @since 0.1
 */
public class CompoundNounFilter extends TokenFilter {

  final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  final PartOfSpeechAttribute posAtt = addAttribute(PartOfSpeechAttribute.class);
  final ReadingAttribute readAtt = addAttribute(ReadingAttribute.class);
  final CompoundPOSAttribute cposAtt = addAttribute(CompoundPOSAttribute.class);
  final CompoundReadingAttribute creadAtt = addAttribute(CompoundReadingAttribute.class);
  final CompoundPronunciationAttribute cproAtt = addAttribute(CompoundPronunciationAttribute.class);
  final StringBuilder compoundTerm, compoundReading, compoundPronunciation;
  private State prevState;
  private final String delimiter;
  private final boolean unknown;

  protected CompoundNounFilter(TokenStream input) {
    this(input, "");
  }

  protected CompoundNounFilter(TokenStream input, String delimiter) {
    this(input, delimiter, true);
  }

  protected CompoundNounFilter(TokenStream input, boolean unknown) {
    this(input, "", unknown);
  }

  /**
   *
   * @param input
   * @param delimiter 複合名詞の中の名詞と名詞の間に挿入する文字列を指定する。
   * @param unknown 未知語を複合名詞の一部とする（true）か否か（false）を指定する。
   *
   */
  protected CompoundNounFilter(TokenStream input, String delimiter, boolean unknown) {
    super(input);
    compoundTerm = new StringBuilder();
    compoundReading = new StringBuilder();
    compoundPronunciation = new StringBuilder();
    this.delimiter = delimiter;
    this.unknown = unknown;
  }

  public boolean incrementToken() throws IOException {
    return nextTerm();
  }

  private boolean nextTerm() throws IOException {
    int count = 0, curStartOffset = 0, curEndOffset = 0;
    compoundTerm.setLength(0);
    compoundReading.setLength(0);
    compoundPronunciation.setLength(0);
    String curPos = null, curIt = null, curIf = null, curBf = null;
    boolean curSf = false;
    if(prevState != null){
      restoreState(prevState);
      prevState = null;
      if(!isNoun(count)){
        return true;
      }
      count = 1;
      curStartOffset = offsetAtt.startOffset();
      curEndOffset = offsetAtt.endOffset();
      compoundTerm.append(termAtt.buffer(), 0, termAtt.length());
      compoundReading.append(readAtt.getReading());
      compoundPronunciation.append(readAtt.getPronunciation());
      curPos = posAtt.getPartOfSpeech();
    }

    while(input.incrementToken()){
      if(isNoun(count) && (count == 0 || curEndOffset == offsetAtt.startOffset())){
        if(count == 0)
          curStartOffset = offsetAtt.startOffset();
        curEndOffset = offsetAtt.endOffset();
        if(compoundTerm.length() > 0){
          compoundTerm.append(delimiter);
          compoundReading.append(delimiter);
          compoundPronunciation.append(delimiter);
        }
        compoundTerm.append(termAtt.buffer(), 0, termAtt.length());
        if(readAtt.getReading() != null)
          compoundReading.append(readAtt.getReading());
        compoundPronunciation.append(readAtt.getPronunciation());
        curPos = posAtt.getPartOfSpeech();
        count++;
      }
      else{   // non-noun or space-char found!
        if(count > 0){
          // set State buffer for the next time
          prevState = captureState().clone();

          cposAtt.setCompoundPOS("複合名詞");
          termAtt.setEmpty().append(compoundTerm.toString());
          offsetAtt.setOffset(curStartOffset, curEndOffset);
          creadAtt.setCompoundReading(compoundReading.toString());
          cproAtt.setCompoundPronunciation(compoundPronunciation.toString());
        }
        return true;
      }
    }

    if(count == 0) return false;
    cposAtt.setCompoundPOS("複合名詞");
    termAtt.setEmpty().append(compoundTerm.toString());
    offsetAtt.setOffset(curStartOffset, curEndOffset);
    creadAtt.setCompoundReading(compoundReading.toString());
    cproAtt.setCompoundPronunciation(compoundPronunciation.toString());
    return true;
  }

  private boolean isNoun(int count){
    String pos = posAtt.getPartOfSpeech();
    if(pos == null) return false;
    if(pos.startsWith("名詞") || (pos.startsWith("UNK") && unknown)) return true;
    return pos.equals("接頭詞-名詞接続") && count == 0 ? true : false;
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    prevState = null;
  }
}
