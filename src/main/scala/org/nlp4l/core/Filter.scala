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

package org.nlp4l.core

import org.apache.lucene.index.Term
import org.apache.lucene.search.{Filter => LuceneFilter, QueryWrapperFilter, TermQuery, MatchAllDocsQuery}

/**
 * Class representing a filter to get a subset of the index. This is a thin wrapper for Lucene Filter.
 *
 * @constructor Create a new Filter instance with given Lucene　Filter.
 *
 * @param luceneFilter the lucene Filter instance
 */
class Filter(val luceneFilter: LuceneFilter)

/**
 * Case class representing a term filter. This wraps QueryWrapperFilter wrapping TermQuery.
 *
 * @constructor Create a new TermFilter instance with given field name and value.
 *
 * @param field the field name for TermQuery
 * @param value the value for TermQuery
 */
case class TermFilter(field: String, value: String) extends Filter(new QueryWrapperFilter(new TermQuery(new Term(field, value))))

/**
 * Case class representing a match all docs filter. This wraps Lucene's QueryWrapperFilter wrapping MatchAllDocsQuery.
 */
case class AllDocsFilter() extends Filter (new QueryWrapperFilter(new MatchAllDocsQuery()))
