import java.io.File

import org.nlp4l.lm._

import com.ibm.icu.text.Transliterator

import scala.io._
import scalax.file.Path

val index = "/tmp/index-transliteration"

// create hmm model index
val trans = Transliterator.getInstance("Katakana-Latin")
val aligner = new StringAligner
// remove index directory before creating it
val p = Path(new File(index))
p.deleteRecursively()
val indexer = new HmmModelIndexer(index)

val file = Source.fromFile("train_data/alpha_katakana.txt", "UTF-8")

file.getLines.foreach{ line: String =>
  val params = line.split(",").map(_.trim)
  val doc = aligner.align(trans.transliterate(params(1)), params(0))
  indexer.addDocument(doc)
}

file.close

indexer.close

// read the model index
val model = HmmModel(index)

// print the model
println("\n=== classes ===")
model.classes.foreach(
  println(_)
)

println("\n=== classNamesDic ===")
model.classNamesDic.foreach{
  println(_)
}

println("\n=== costInitialState ===")
model.costInitialState.foreach{
  println(_)
}

println("\n=== costConnection ===")
val tableSize = model.classes.size
print("   ")
for(i <- 0 to tableSize - 1){
  print("%10s".format(model.classes(i)._1))
}
println
for(i <- 0 to tableSize - 1){
  print("%4s ".format(model.classes(i)._1))
  for(j <- 0 to tableSize - 1){
    print("%8d  ".format(model.costConnection(i)(j)))
  }
  println
}

println("\n=== words ===")
model.words.foreach{
  println(_)
}

println("\n=== wordDic ===")
model.wordDic.foreach{
  println(_)
}

println("\n=== wordClasses ===")
model.wordClasses.foreach{
  println(_)
}

println("\n=== wordDic(tempDic) ===")
model.tempDic.foreach{ e =>
  println(e._1)
  e._2.foreach{ g =>
    println("\t%s(%d) %d".format(model.classes(g._1), g._1, g._2))
  }
}

println("\n=== tokenizer test ===")
val tokenizer = HmmTokenizer(model)

tokenizer.tokens(trans.transliterate("パナソニック"))
