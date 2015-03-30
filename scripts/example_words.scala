import java.nio.file.FileSystems

import org.apache.lucene.analysis.ja.JapaneseAnalyzer
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core.{RawReader, ISearcher, TermFilter, AllDocsFilter}
import org.apache.lucene.index._
import org.apache.lucene.search.{MatchAllDocsQuery, QueryWrapperFilter, TermQuery}
import org.nlp4l.stats.WordCounts

// TermCounts example
val indexDir = "/tmp/index-ldcc"  // path to livedoor news corpus index directory
val path = FileSystems.getDefault.getPath(indexDir);
//val reader = DirectoryReader.open(FSDirectory.open(path))
val maxSize = 10000  // 考慮する最大term数(出現頻度の高い順に上位何件まで見るか)
val field = "body"   // 対象フィールド名

val reader = RawReader(indexDir)
val searcher = ISearcher(reader)

// ex. "sports-watch" カテゴリに出現する単語とその出現頻度を算出
// Searcher で絞り込んだドキュメント集合について、Lucene API を使って出現するtermとfrequencyを数え上げ
// "sports-watch" カテゴリに出現する単語Top100
val subset1 = reader.subset(TermFilter("cat", "sports-watch"))
val counts1Top100 = WordCounts.count(reader, field, Set.empty[String], subset1, 100, new Analyzer(new JapaneseAnalyzer()))
counts1Top100.toSeq.sortWith((a, b) => a._2 > b._2).foreach(println)

// ex. "dokujo-tsushin" カテゴリに出現する単語とその出現頻度を算出
// "dokujo-tsuushin" カテゴリに出現する単語Top100
val subset2 = reader.subset(TermFilter("cat", "dokujo-tsushin"))
val counts2Top100 = WordCounts.count(reader, field, Set.empty[String], subset2, 100, new Analyzer(new JapaneseAnalyzer()))
counts2Top100.toSeq.sortWith((a, b) => a._2 > b._2).foreach(println)

// ex. "sports-watch" カテゴリだけに出現する単語 (適当に100個)
// XXX: 差集合をとった後に、先に計算した出現頻度で並べ直すことも(少しがんばれば)可能
val counts1 = WordCounts.count(reader, field, Set.empty[String], subset1, -1, new Analyzer(new JapaneseAnalyzer()))
val counts2 = WordCounts.count(reader, field, Set.empty[String], subset2, -1, new Analyzer(new JapaneseAnalyzer()))
(counts1.keys.toSet &~ counts2.keys.toSet).take(100)

// ex. "dokujo-tsushin" カテゴリだけに出現する単語 (適当に100個)
(counts2.keys.toSet &~ counts1.keys.toSet).take(100)

// ex. インデックス全体の単語と出現頻度を算出 (特定のドキュメント集合を与えない場合)
// XXX: 全体の統計情報と、各カテゴリごとの統計情報から、「特定のカテゴリに出現しやすいterm」を抽出するなども(たぶん)易しいはず...
val alldocs = reader.subset(AllDocsFilter())
val countsAll = WordCounts.count(reader, field, Set.empty[String], alldocs, -1, new Analyzer(new JapaneseAnalyzer()))

reader.close
