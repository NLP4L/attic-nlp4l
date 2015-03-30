import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import java.nio.file.FileSystems
import org.apache.lucene.index._
import org.apache.lucene.search.TermQuery
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core._

import scalax.file.Path
import scalax.file.PathSet

val index = "/tmp/index-ceeaus"

def lines(fl: Path, encoding: String): List[String] = {
  val is = new FileInputStream(fl.path)
  val r = new InputStreamReader(is, encoding)
  val br = new BufferedReader(r)
  var result: List[String] = Nil

  try{
    var line = br.readLine()
    while(line != null){
      result = result :+ line
      line = br.readLine()
    }
    result
  }
  finally{
    br.close
    r.close
    is.close
  }
}

def document(fl: Path, ja: Boolean): Document = {
  val ps: Array[String] = fl.path.split("/")
  val file = ps(3)
  val typ = ps(2)
  val cat = if(file.indexOf("smk") >= 0) "smk" else "ptj"   // smoking or part time job
  val encoding = if(ja) "sjis" else "UTF-8"
  val body = lines(fl, encoding)
  Document(Set(
    Field("file", file), Field("type", typ), Field("cat", cat),
    Field(if(ja) "body_ja" else "body_en", body)
  ))
}

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// define a schema for the index
//val analyzerEn = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer())
val analyzerEn = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer(null.asInstanceOf[org.apache.lucene.analysis.util.CharArraySet]))
val analyzerJa = Analyzer(new org.apache.lucene.analysis.ja.JapaneseAnalyzer())
val fieldTypes = Map(
  "file" -> FieldType(null, true, true),
  "type" -> FieldType(null, true, true),
  "cat" -> FieldType(null, true, true),
  "body_en" -> FieldType(analyzerEn, true, true, true, true),   // set termVectors and termPositions to true
  "body_ja" -> FieldType(analyzerJa, true, true, true, true)    // set termVectors and termPositions to true
)
val analyzerDefault = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer())
val schema = Schema(analyzerDefault, fieldTypes)

// write documents into an index
val writer = IWriter(index, schema)

val c: PathSet[Path] = Path("corpora", "CEEAUS").children()
// write English docs
c.filter(e => e.name.indexOf("CJEJUS")<0 && e.name.indexOf("PLAIN")<0).foreach( f =>
  f.children().filter( g => g.name.indexOf("(1)") < 0 && g.name.endsWith(".txt")).foreach(h => writer.write(document(h, false)))
)
// write Japanese docs
c.filter(e => e.name.indexOf("CJEJUS")>=0).foreach( f =>
  f.children().filter( g => g.name.indexOf("(1)") < 0 && g.name.endsWith(".txt")).foreach(h => writer.write(document(h, true)))
)
writer.close

// search
val searcher = ISearcher(index)
val results = searcher.search(query=new TermQuery(new Term("body_ja", "喫煙")), rows=10)

results.foreach(doc => {
  printf("[DocID] %d: %s\n", doc.docId, doc.get("file"))
})
