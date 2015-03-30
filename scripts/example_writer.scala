import java.nio.file.FileSystems
import org.apache.lucene.index._
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.FSDirectory
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core._

val index = "/tmp/myindex"

// define a schema for the index
val bodyAnalyzer = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer())
val fieldTypes = Map("id" -> FieldType(null, true, true), "body" -> FieldType(bodyAnalyzer, true, true))
val schema = Schema(bodyAnalyzer, fieldTypes)

// write documents into an index
val writer = IWriter(index, schema)
writer.write(Document(Set(Field("id", "1"), Field("body", "Hello, Lucene"))))
writer.write(Document(Set(Field("id", "2"), Field("body", "Hello, Solr"))))
writer.close

// search
val path = FileSystems.getDefault.getPath(index);
val reader = DirectoryReader.open(FSDirectory.open(path))
val searcher = ISearcher(index)

searcher.search(query=new TermQuery(new Term("body", "hello")), rows = 10).foreach(d => {
  printf("[DocID] %d: %s\n", d.docId, d.get("body").get.values)
})

reader.close
