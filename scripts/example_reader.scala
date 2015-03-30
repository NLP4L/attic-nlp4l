import java.nio.file._
import org.apache.lucene.index.Term
import org.apache.lucene.search.{PhraseQuery, TermQuery, QueryWrapperFilter}
import org.nlp4l.core.RawReader

val idxDir = "/opt/solr/solr-5.0.0-SNAPSHOT/ldcc/collection1/data/index"
val path = FileSystems.getDefault.getPath(idxDir)

// IndexReaderのラッパーもどき
val reader = new RawReader(path)

// インデックス情報
val numDocs = reader.numDocs
val maxDoc = reader.maxDoc
val hasDeletions = reader.hasDeletions
val numDeletedDocs = reader.numDeletedDocs

println("\n----")

// フィールド情報
val numFields = reader.numFields
val fields = reader.fields.map(f => (f.number, f.name, f.terms.uniqTerms))

println("\n----")

// cat, title, body の各フィールド中の、出現回数の多いトップ10単語
val topTerms = List("cat", "title", "body").map(f => (f, reader.topTermsByDocFreq(f, 10)))

println("\n----")

/* Luceneインデックスから、単語の共起確率を計算してみるテスト */
// "ネット" を含むドキュメント(ID)の集合
val docs1 = reader.field("body").term("テレビ").docIds.toSet

// "テレビ" を含むドキュメント(ID)の集合
val docs2 = reader.field("body").term("テレビ").docIds.toSet

// "新聞" を含むドキュメント(ID)の集合
val docs3 = reader.field("body").term("新聞").docIds.toSet

val docs1_inter_docs2 = docs1 & docs2
val docs1_inter_docs3 = docs1 & docs3
val docs2_inter_docs3 = docs2 & docs3
println("'ネット'と'テレビ'の共起確率" + (docs1_inter_docs2.size).toFloat / (docs1.size + docs2.size - docs1_inter_docs2.size).toFloat)
println("'ネット'と'新聞'の共起確率" + (docs1_inter_docs3.size).toFloat / (docs1.size + docs3.size - docs1_inter_docs3.size).toFloat)
println("'テレビ'と'新聞'の共起確率" + (docs2_inter_docs3.size).toFloat / (docs2.size + docs3.size - docs2_inter_docs3.size).toFloat)

println("\n----")

// term vector (term vectors must be indexed)
val vec1 = reader.getTermVector(1000,"title")  // -> null
val vec2 = reader.getTermVector(1000,"body2")  // -> return terms

// positions and offsets info from index or term vector (if exists)
val docWithPositions = reader.field("title").term("映画").map(d => (d.docId, d.posAndOffsets))

println("\n----")

// get index subset (doc id set) by filtering
// needs lucene Filter object
val pq = new PhraseQuery()
pq.add(new Term("body", "イタリアン"))
pq.add(new Term("body", "レストラン"))
val filter = new QueryWrapperFilter(pq)
val subset = reader.subset(filter)
println(subset)