package org.apache.lucene.analysis.brown;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

import java.lang.Override;

public class PartOfSpeechAttributeImpl extends AttributeImpl implements PartOfSpeechAttribute, Cloneable {

  private String pos;

  @Override
  public String getPartOfSpeech(){
    return pos;
  }

  @Override
  public void setPartOfSpeech(String partOfSpeech){
    pos = partOfSpeech;
  }

  @Override
  public void clear() {
    pos = null;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    PartOfSpeechAttribute t = (PartOfSpeechAttribute) target;
    t.setPartOfSpeech(pos);
  }

  @Override
  public void reflectWith(AttributeReflector reflector) {
    String partOfSpeech = getPartOfSpeech();
    reflector.reflect(PartOfSpeechAttribute.class, "partOfSpeech", partOfSpeech);
  }
}
