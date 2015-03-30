import java.nio.file.FileSystems

import org.apache.lucene.index._
import org.apache.lucene.search._
import org.apache.lucene.store._
import org.nlp4l.core.analysis._
import org.nlp4l.core._

val index = "/tmp/index-ceeaus"
val reader = RawReader(index)

// Table 1
def docsPerSubCorpus(corpus: String): Int = {
  reader.termDocs("type", corpus).docFreq
}

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

def termsPerSubCorpus(field: String, corpus: String): Int = {
  var count = 0
  val filter = new TermQuery(new Term("type", corpus))
  val searcher = ISearcher(reader)

  reader.terms(field).foreach{ t =>
    val bq: BooleanQuery = new BooleanQuery()
    bq.add(filter, BooleanClause.Occur.MUST)
    bq.add(new TermQuery(new Term(field, t.text)), BooleanClause.Occur.MUST)
    val result = searcher.search(bq)
    if(result.nonEmpty) count += 1
  }
  count
}

// pass null to constructor to avoid using stop words
val analyzerEn = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer(null.asInstanceOf[org.apache.lucene.analysis.util.CharArraySet]))
val analyzerJa = Analyzer(new org.apache.lucene.analysis.ja.JapaneseAnalyzer())

println("\n\nCh1  Table-1")
println("=========================================================================")
println("\t\tCEEJUS\t\tCEECUS\t\tCEENAS\t\tCJEJUS")
println("-------------------------------------------------------------------------")
println("docs\t\t%,8d\t%,8d\t%,8d\t%,8d".format(
  docsPerSubCorpus("CEEJUS"),
  docsPerSubCorpus("CEECUS"),
  docsPerSubCorpus("CEENAS"),
  docsPerSubCorpus("CJEJUS")))
println("totalTerms\t%,8d\t%,8d\t%,8d\t%,8d".format(
  totalTermsPerSubCorpus("body_en", "CEEJUS", analyzerEn),
  totalTermsPerSubCorpus("body_en", "CEECUS", analyzerEn),
  totalTermsPerSubCorpus("body_en", "CEENAS", analyzerEn),
  totalTermsPerSubCorpus("body_ja", "CJEJUS", analyzerJa)
))
println("terms\t\t%,8d\t%,8d\t%,8d\t%,8d".format(
  termsPerSubCorpus("body_en", "CEEJUS"),
  termsPerSubCorpus("body_en", "CEECUS"),
  termsPerSubCorpus("body_en", "CEENAS"),
  termsPerSubCorpus("body_ja", "CJEJUS")
))

reader.close
