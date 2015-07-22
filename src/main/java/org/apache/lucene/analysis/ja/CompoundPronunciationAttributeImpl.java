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

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;

public class CompoundPronunciationAttributeImpl extends AttributeImpl implements CompoundPronunciationAttribute, Cloneable {

  private String compoundPronunciation;

  @Override
  public String getCompoundPronunciation() {
    return compoundPronunciation;
  }

  @Override
  public void setCompoundPronunciation(String compoundPronunciation) {
    this.compoundPronunciation = compoundPronunciation;
  }

  @Override
  public void clear() {
    compoundPronunciation = null;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    CompoundPronunciationAttribute t = (CompoundPronunciationAttribute)target;
    t.setCompoundPronunciation(compoundPronunciation);
  }

  @Override
  public void reflectWith(AttributeReflector reflector){
    String compoundPronunciation = getCompoundPronunciation();
    reflector.reflect(CompoundPronunciationAttribute.class, "compoundPronunciation", compoundPronunciation);
  }
}
