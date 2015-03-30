import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import java.nio.file.FileSystems
import org.apache.lucene.index._
import org.apache.lucene.search.TermQuery
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core.analysis.AnalyzerBuilder
import org.nlp4l.core._

import scalax.file.Path
import scalax.file.PathSet

val index = "/tmp/index-ceeaus-all"

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
  val cat = "all"
  val encoding = if(ja) "sjis" else "UTF-8"
  val body = lines(fl, encoding)
  val body_set = if(ja) Set(Field("body_ja", body)) else Set(Field("body_en", body), Field("body_ws", body))
  Document(Set(
    Field("file", file), Field("type", typ), Field("cat", cat)) ++ body_set
  )
}

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// define a schema for the index
//val analyzerEn = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer())
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
val schema = Schema(analyzerDefault, fieldTypes)

// write documents into an index
val writer = IWriter(index, schema)

val c: PathSet[Path] = Path("corpora", "CEEAUS", "PLAIN").children()
// write English docs
c.filter(e => e.name.indexOf("cjejus")<0 && e.name.endsWith(".txt")).foreach(g => writer.write(document(g, false)))
// write English docs
c.filter(e => e.name.indexOf("cjejus")>=0 && e.name.endsWith(".txt")).foreach(g => writer.write(document(g, true)))
writer.close

// search test
val searcher = ISearcher(index)
val results = searcher.search(query=new TermQuery(new Term("body_ja", "喫煙")), rows=10)

results.foreach(doc => {
  printf("[DocID] %d: %s\n", doc.docId, doc.get("file"))
})

// search test for ch4
val results2 = searcher.search(query=new TermQuery(new Term("body_ws", "still,")), rows=10)

results2.foreach(doc => {
  printf("[DocID] %d: %s\n", doc.docId, doc.get("file"))
})
