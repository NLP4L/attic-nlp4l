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

public class CompoundReadingAttributeImpl extends AttributeImpl implements CompoundReadingAttribute, Cloneable {

  private String compoundReading;

  @Override
  public String getCompoundReading() {
    return compoundReading;
  }

  @Override
  public void setCompoundReading(String compoundReading) {
    this.compoundReading = compoundReading;
  }

  @Override
  public void clear() {
    compoundReading = null;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    CompoundReadingAttribute t = (CompoundReadingAttribute)target;
    t.setCompoundReading(compoundReading);
  }

  @Override
  public void reflectWith(AttributeReflector reflector){
    String compoundReading = getCompoundReading();
    reflector.reflect(CompoundReadingAttribute.class, "compoundReading", compoundReading);
  }
}
