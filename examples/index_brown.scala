import java.io.File
import java.nio.file.FileSystems
import org.apache.lucene.index._
import org.apache.lucene.search.TermQuery
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core._
import scala.io._
import scalax.file.Path
import scalax.file.PathSet

val index = "/tmp/index-brown"

def removePos(line: String): String = {
  val words = line.split(" ")
  words.map{ word =>
    val idx = word.lastIndexOf('/')
    if(idx >= 0) word.substring(0, idx) else word
  }.mkString(" ")
}

def document(file: Path, catsmap: Map[String, String]): Document = {
  val ps: Array[String] = file.path.split("/")
  val fl = ps(3)
  val cat = catsmap.getOrElse(fl, "")
  val bodyPos = file.lines().filterNot(_.length()==0).toList
  val body = bodyPos.map(removePos(_))
  Document(Set(
    Field("file", fl), Field("cat", cat), Field("body_pos", bodyPos), Field("body_pos_nn", bodyPos), Field("body", body))
  )
}

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// define a schema for the index
/*
val analyzerBr = Analyzer(new org.apache.lucene.analysis.brown.BrownCorpusAnalyzer())
val analyzerEn = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer(null.asInstanceOf[org.apache.lucene.analysis.util.CharArraySet]))
val fieldTypes = Map(
  "file" -> FieldType(null, true, true),
  "cat" -> FieldType(null, true, true),
  "body_pos" -> FieldType(analyzerBr, true, true, true, true),   // set termVectors and termPositions to true
  "body" -> FieldType(analyzerEn, true, true, true, true)        // set termVectors and termPositions to true
)
val schema = Schema(analyzerEn, fieldTypes)
*/
// load schema from file
val schema = SchemaLoader.load("examples/schema/brown.conf")

// write documents into an index
val writer = IWriter(index, schema)

// read category list
val cats = Source.fromFile("corpora/brown/brown/cats.txt")
val catsmap = cats.getLines().map{ line =>
  val ps = line.split(" ")
  (ps(0),ps(1))
}.toMap

val c: PathSet[Path] = Path("corpora", "brown", "brown").children()
// write articles
c.filter(e => e.name.startsWith("c") && e.name.length() == 4).foreach(f => writer.write(document(f, catsmap)))
writer.close

// search test
val searcher = ISearcher(index)
val results = searcher.search(query=new TermQuery(new Term("body", "smoothly")), rows=10)

results.foreach(doc => {
  printf("[DocID] %d: %s\n", doc.docId, doc.get("file"))
})
