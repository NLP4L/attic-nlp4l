import org.apache.lucene.index.Term
import org.apache.lucene.search.{SortField, Sort, TermQuery}
import org.nlp4l.core.{Field, ISearcher, RawReader}

// ISearcher example
val idxDir = "/Users/tomoko/solr/ldcc/collection1/data/index"  // path to livedoor news corpus index directory
val reader = RawReader(idxDir)
val searcher = ISearcher(reader)

// クエリと返却件数を指定して検索
val res1 = searcher.search(query=new TermQuery(new Term("title", "旅行")), rows=10)

// 検索結果を出力
res1.foreach(doc => {
  println(doc.docId, doc.get("title"))
})

// 検索結果のカテゴリごとのドキュメント数を数え上げ (簡単ファセット)
// TODO 動かない...
//val facets = res1.map(_.get("cat").getOrElse(Field("cat","***")).values(0)).foldLeft(scala.collection.mutable.Map.empty[String, Int]){(m,c) => m += (c -> (m.getOrElse(c,0)+1))}

println("\n----")
// クエリと返却件数、ソートを指定して検索
// ソート対象のフィールドは、doc values を保持しておく必要あり
val res2 = searcher.search(query=new TermQuery(new Term("title", "旅行")), rows=10, sort=new Sort(new SortField("date", SortField.Type.LONG, true)))

println("\n----")
// ex. 検索結果ドキュメントと、あわせて対応する Term Vector を取得
val resWithTv = searcher.search(query=new TermQuery(new Term("title", "旅行")), rows=10).map(doc => (doc, reader.getTermVector(doc.docId, "body2")))
