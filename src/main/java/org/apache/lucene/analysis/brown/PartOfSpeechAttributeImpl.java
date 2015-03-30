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
