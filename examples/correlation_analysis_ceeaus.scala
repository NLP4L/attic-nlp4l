import org.nlp4l.core._
import org.nlp4l.core.analysis._
import org.nlp4l.stats._

val index = "/tmp/index-ceeaus-all"

def schema(): Schema = {
  val analyzerEn = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer(null.asInstanceOf[org.apache.lucene.analysis.util.CharArraySet]))
  val builder = AnalyzerBuilder()
  builder.withTokenizer("whitespace")
  builder.addTokenFilter("lowerCase")
  val analyzerWs = builder.build
  val analyzerJa = Analyzer(new org.apache.lucene.analysis.ja.JapaneseAnalyzer())
  val fieldTypes = Map(
    "file" -> FieldType(null, true, true),
    "type" -> FieldType(null, true, true),
    "cat" -> FieldType(null, true, true),
    "body_en" -> FieldType(analyzerEn, true, true, true, true),   // set termVectors and termPositions to true
    "body_ws" -> FieldType(analyzerWs, true, true, true, true),   // set termVectors and termPositions to true
    "body_ja" -> FieldType(analyzerJa, true, true, true, true)    // set termVectors and termPositions to true
  )
  val analyzerDefault = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer())
  Schema(analyzerDefault, fieldTypes)
}

val reader = IReader(index, schema())

val docSetJUS = reader.subset(TermFilter("file", "ceejus_all.txt"))
val docSetCUS = reader.subset(TermFilter("file", "ceecus_all.txt"))
val docSetNAS = reader.subset(TermFilter("file", "ceenas_all.txt"))


val words = List("besides,", "nevertheless,", "also,", "moreover,", "however,", "therefore,", "so,")

val wcJUS = WordCounts.count(reader, "body_ws", words.toSet, docSetJUS)
val wcCUS = WordCounts.count(reader, "body_ws", words.toSet, docSetCUS)
val wcNAS = WordCounts.count(reader, "body_ws", words.toSet, docSetNAS)

println("\n\n\nword counts")
println("========================================")
println("\tword\tCEEJUS\tCEECUS\tCEENAS")
words.foreach{ e =>
  println("%10s\t%,6d\t%,6d\t%,6d".format(e, wcJUS.getOrElse(e, 0), wcCUS.getOrElse(e, 0), wcNAS.getOrElse(e, 0)))
}

val lj = List( ("CEEJUS", wcJUS), ("CEECUS", wcCUS), ("CEENAS", wcNAS) )
println("\n\n\nCorrelation Coefficient")
println("================================")
println("\tCEEJUS\tCEECUS\tCEENAS")
lj.foreach{ ej =>
  print("%s".format(ej._1))
  lj.foreach{ ei =>
    print("\t%5.3f".format(Stats.correlationCoefficient(words.map(ej._2.getOrElse(_, 0.toLong)), words.map(ei._2.getOrElse(_, 0.toLong)))))
  }
  println
}

reader.close
