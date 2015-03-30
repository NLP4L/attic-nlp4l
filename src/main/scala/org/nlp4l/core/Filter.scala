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
