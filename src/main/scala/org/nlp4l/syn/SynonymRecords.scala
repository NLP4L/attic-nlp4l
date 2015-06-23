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

package org.nlp4l.syn

class SynonymRecords(val synFile: String, val outFile: String, val headerComments: Seq[String],
    val uniqueRecords: Seq[Seq[String]]) {
  
  def this(synFile: String, input: (Seq[String], Seq[Seq[String]])) = {
    this(synFile, "%s_checked".format(synFile), input._1, SynonymCommon.getUniqueRecords(input._2, List()))
  }
}
