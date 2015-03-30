package org.apache.lucene.analysis.brown;

import org.apache.lucene.util.Attribute;

public interface PartOfSpeechAttribute extends Attribute {
  public String getPartOfSpeech();
  public void setPartOfSpeech(String partOfSpeech);
}
