import java.nio.file.FileSystems

import org.apache.lucene.index._
import org.apache.lucene.search.{Filter => LuceneFilter}
import org.apache.lucene.search._
import org.apache.lucene.store._
import org.nlp4l.core.analysis._
import org.nlp4l.core._
import org.nlp4l.stats.WordCounts
import scala.collection.mutable

val index = "/tmp/index-ceeaus"
val reader = RawReader(index)

def reusableFilter(corpus: String): (LuceneFilter, Seq[Document]) = {
  val tq = new TermQuery(new Term("type", corpus))
  val f = new CachingWrapperFilter(new QueryWrapperFilter(tq))
  val searcher = ISearcher(reader)
  (f, searcher.search(filter=f, rows=100000))
}

def reusableFilterAll(): (LuceneFilter, Seq[Document]) = {
  val f = new CachingWrapperFilter(new QueryWrapperFilter(new MatchAllDocsQuery))
  val searcher = ISearcher(reader)
  (f, searcher.search(filter=f, rows=100000))
}

// copied from ch1
def totalTermsPerSubCorpus(field: String, corpus: String, analyzer: Analyzer): Int = {
  var count = 0
  reader.termDocs("type", corpus).docIds.foreach{ t =>
    val document = reader.document(t)
    val v: Option[Field] = document.get.map.get(field)
    if(v.nonEmpty){
      v.get.values.foreach(count += analyzer.tokens(_).size)
    }
  }
  count
}

// section 2.4.2 per million words
def pmwPerSubCorpus(testTerms: Set[String], docIds: Iterable[Int], field: String, corpus: String, analyzer: Analyzer): Map[String, Int] = {
  val liveDocs = MultiFields.getLiveDocs(reader.ir)

  val termsMap = mutable.Map.empty[String, Int]
  for (docId <- docIds) {
    val terms = reader.ir.getTermVector(docId, field);
    if(terms != null){
      val te = terms.iterator(null)
      var term = te.next
      while (term != null) {
        val ts: String = term.utf8ToString
        if(testTerms.contains(ts)){
          termsMap += (ts -> (termsMap.getOrElse(ts, 0) + te.totalTermFreq.toInt))
        }
        term = te.next
      }
    }
  }

  val totalTerms = totalTermsPerSubCorpus(field, corpus, analyzer)

  // calculate per million words
  termsMap.map{ e => e._1 -> (termsMap.getOrElse(e._1, 0).toFloat / totalTerms.toFloat * 1000000).toInt}.toMap
}

// pass null to constructor to avoid using stop words
val analyzerEn = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer(null.asInstanceOf[org.apache.lucene.analysis.util.CharArraySet]))
val analyzerJa = Analyzer(new org.apache.lucene.analysis.ja.JapaneseAnalyzer())

val rfCEEJUS = reusableFilter("CEEJUS")
val rfCEECUS = reusableFilter("CEECUS")
val rfCEENAS = reusableFilter("CEENAS")
val rfCJEJUS = reusableFilter("CJEJUS")

val rfAll = reusableFilterAll()
val alldocs = reader.subset(new QueryWrapperFilter(new MatchAllDocsQuery))
val allWords = WordCounts.count(reader, "body_en", Set.empty[String], alldocs, -1, analyzerJa)

val allTestWords = allWords.keys.take(10).toSet
//val allDocSet = rfAll._2.map(_.docId)
val pmwCEEJUS = pmwPerSubCorpus(allTestWords, rfCEEJUS._2.map(_.docId), "body_en", "CEEJUS", analyzerEn)
val pmwCEECUS = pmwPerSubCorpus(allTestWords, rfCEECUS._2.map(_.docId), "body_en", "CEECUS", analyzerEn)
val pmwCEENAS = pmwPerSubCorpus(allTestWords, rfCEENAS._2.map(_.docId), "body_en", "CEENAS", analyzerEn)

println("\n\n\nper million words (adjusted frequency)")
println("======================================")
println("\tword\tCEEJUS\t\tCEECUS\t\tCEENAS")
pmwCEEJUS.foreach{ e =>
  println("%10s\t%,8d\t%,8d\t%,8d".format(e._1, pmwCEEJUS.getOrElse(e._1, 0), pmwCEECUS.getOrElse(e._1, 0), pmwCEENAS.getOrElse(e._1, 0)))
}

reader.close
