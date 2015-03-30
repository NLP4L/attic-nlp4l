import java.io.File
import java.nio.file.FileSystems
import org.apache.lucene.index._
import org.apache.lucene.search.TermQuery
import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core._

import scala.collection.mutable.ArrayBuffer
import scala.util.matching._
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.XML

import scalax.file.Path
import scalax.file.PathSet

val p1: Regex = """\<([A-Z]+)\>(.*)\</([A-Z]+)\>""".r
val pTitleBegin: Regex = """\*+<TITLE\>(.*)""".r
val pTitleEnd: Regex = """\</TITLE\>.+""".r
val pTextBegin: Regex = """\<TEXT TYPE\="(\w+)"\>\&#2;""".r
val pTextBegin2: Regex = """\<TEXT\>\&#2;""".r
val pTextEnd: Regex = """\&#3;\</BODY\>\</TEXT\>""".r
val pTextEnd2: Regex = """\</TEXT\>""".r
val pReutersBegin: Regex = """\<REUTERS TOPICS="(.*)" LEWISSPLIT="(.*)" CGISPLIT="(.*)" OLDID="(.*)" NEWID="(.*)"\>""".r
val pReutersEnd: Regex = """\</REUTERS\>""".r
val pUnknownBegin: Regex = """\<UNKNOWN\>.*""".r
val pUnknownEnd: Regex = """(.*)\</UNKNOWN\>""".r
val pDatelineBody: Regex = """(.*)\<DATELINE\>(.*)\</DATELINE\>\<BODY\>(.*)""".r
val pDatelineBegin: Regex = """\<DATELINE\>(.*)""".r
val pDatelineEnd: Regex = """(.*)\</DATELINE\>\<BODY\>(.*)""".r

// scala.xml.Utility cannot be used because str is partially escaped (e.g. str="&lt;Teck Hock and Co (Pte) Ltd>")
def xmlescape(str: String): String = {
  if(str.indexOf("<D>") >=0 && str.indexOf("</D>") >= 0) str
  else str.replaceAll(">", "&gt;").replaceAll("'", "&apos;").replaceAll("\"", "&quot;").replaceAll("&#.+;", "")
}

def reproduce1(tag: String, value: String): String = {
  "<%s>%s</%s>".format(tag, value, tag)
}

def normalizeDate(date: String): String = {
  if(date.indexOf('&') >= 0) date.substring(0, date.indexOf('&'))
  else if(date.indexOf('#') >= 0) date.substring(0, date.indexOf('#')).trim()
  else if(date.indexOf("605:") >= 0) date.replaceAll("605", "05")
  else date
}

def xmlline(line: String): String = {
  line match {
    case p1("DATE", date, "DATE") => {
      val normDate = normalizeDate(date)
      reproduce1("DATE", xmlescape(normDate))
    }
    case p1("UNKNOWN", unknown, "UNKNOWN") => { reproduce1("UNKNOWN", xmlescape(unknown)) }
    case p1("MKNOTE", mknote, "MKNOTE") => { reproduce1("MKNOTE", xmlescape(mknote)) }
    case p1("TOPICS", topics, "TOPICS") => { reproduce1("TOPICS", xmlescape(topics)) }
    case p1("PEOPLE", people, "PEOPLE") => { reproduce1("PEOPLE", xmlescape(people)) }
    case p1("ORGS", orgs, "ORGS") => { reproduce1("ORGS", xmlescape(orgs)) }
    case p1("EXCHANGES", exchanges, "EXCHANGES") => { reproduce1("EXCHANGES", xmlescape(exchanges)) }
    case p1("COMPANIES", companies, "COMPANIES") => { reproduce1("COMPANIES", xmlescape(companies)) }
    case p1("AUTHOR", author, "AUTHOR") => { reproduce1("AUTHOR", xmlescape(author)) }
    case p1("PLACES", places, "PLACES") => { reproduce1("PLACES", xmlescape(places)) }
    case p1("TITLE", title, "TITLE") => { reproduce1("TITLE", xmlescape(title)) }
    case pTextBegin(typ) => { """<TEXT TYPE="%s">""".format(typ) }
    case pTextBegin2() => { "<TEXT>" }
    case pTextEnd() => { "</BODY></TEXT>" }
    case pTextEnd2() => { "</TEXT>" }
    case pTitleBegin(title) => { """<TITLE>%s""".format(xmlescape(title)) }
    case pTitleEnd() => { "</TITLE>" }
    case pReutersBegin(topics, lewis, cgi, oldid, newid) => {
      """<REUTERS TOPICS="%s" LEWISSPLIT="%s" CGISPLIT="%s" OLDID="%s" NEWID="%s">""".format(topics, lewis, cgi, oldid, newid)
    }
    case pReutersEnd() => { "</REUTERS>" }
    case pUnknownBegin() => { "<UNKNOWN>" }
    case pUnknownEnd(unknown) => { """%s</UNKNOWN>""".format(xmlescape(unknown)) }
    case pDatelineBody(pre, dateline, body) => { """%s<DATELINE>%s</DATELINE><BODY>%s""".format(xmlescape(pre), xmlescape(dateline), xmlescape(body)) }
    case pDatelineBegin(dateline) => { """<DATELINE>%s""".format(xmlescape(dateline)) }
    case pDatelineEnd(dateline, body) => { """%s</DATELINE><BODY>%s""".format(xmlescape(dateline), xmlescape(body)) }
    case _ => { xmlescape(line) }
  }
}

def articles(file: Path): NodeSeq = {
  val lines = file.lines().drop(1).toList     // use drop(1)  to avoid the line <!DOCTYPE lewis SYSTEM "lewis.dtd">
  XML.loadString(lines.map(xmlline(_)).mkString("<ROOT>", "\n", "</ROOT>")) \ "REUTERS"   // use "\n" instead of "" for easy debugging
}

def dValue(node: NodeSeq): List[String] = {
  if(node != null){
    val ds = node \ "D"
    if(ds.size > 0) ds.map(_.text).toList
    else null
  }
  else null
}

def multiValued(node: NodeSeq): List[String] = {
  val text = node.text
  if(text != null){
    text.split("\n").toList
  }
  else null
}

val index = "/tmp/index-reuters"

def document(article: Node): Document = {
  val fields: ArrayBuffer[(String, Any)] = ArrayBuffer.empty[(String, Any)]
  fields.append(("date", (article \ "DATE").text.trim))
  fields.append(("newid", (article \ "@NEWID").text))
  fields.append(("oldid", (article \ "@OLDID").text))
  fields.append(("cgisplit", (article \ "@CGISPLIT").text))
  fields.append(("lewissplit", (article \ "@LEWISSPLIT").text))
  fields.append(("topics", dValue(article \ "TOPICS")))
  fields.append(("places", dValue(article \ "PLACES")))
  fields.append(("people", dValue(article \ "PEOPLE")))
  fields.append(("orgs", dValue(article \ "ORGS")))
  fields.append(("exchanges", dValue(article \ "EXCHANGES")))
  fields.append(("companies", dValue(article \ "COMPANIES")))
  fields.append(("unknown", multiValued(article \ "UNKNOWN")))
  fields.append(("title", multiValued(article \\ "TITLE")))
  fields.append(("dateline", multiValued(article \\ "DATELINE")))
  fields.append(("body", multiValued(article \\ "BODY")))
  Document(
    fields.filterNot(e => e._2 == null).map{
      f =>
        if(f._2.isInstanceOf[String]) Field(f._1, f._2.asInstanceOf[String])
        else Field(f._1, f._2.asInstanceOf[List[String]])
    }.toSet
  )
}

// delete existing Lucene index
val p = Path(new File(index))
p.deleteRecursively()

// define a schema for the index
val analyzerEn = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer())
val fieldTypes = Map(
  "date" -> FieldType(null, true, true),    // TODO: use date type?
  "newid" -> FieldType(null, true, true),   // TODO: use int type?
  "oldid" -> FieldType(null, true, true),   // TODO: use int type?
  "cgisplit" -> FieldType(null, true, true),
  "lewissplit" -> FieldType(null, true, true),
  "topics" -> FieldType(analyzerEn, true, true, true, true), // set termVectors and termPositions to true
  "places" -> FieldType(analyzerEn, true, true, true, true), // set termVectors and termPositions to true
  "people" -> FieldType(analyzerEn, true, true, true, true), // set termVectors and termPositions to true
  "orgs" -> FieldType(analyzerEn, true, true, true, true), // set termVectors and termPositions to true
  "exchanges" -> FieldType(analyzerEn, true, true, true, true), // set termVectors and termPositions to true
  "companies" -> FieldType(analyzerEn, true, true, true, true), // set termVectors and termPositions to true
  "unknown" -> FieldType(analyzerEn, true, true, true, true), // set termVectors and termPositions to true
  "title" -> FieldType(analyzerEn, true, true, true, true), // set termVectors and termPositions to true
  "dateline" -> FieldType(analyzerEn, true, true, true, true), // set termVectors and termPositions to true
  "body" -> FieldType(analyzerEn, true, true, true, true)   // set termVectors and termPositions to true
)
val analyzerDefault = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer())
val schema = Schema(analyzerDefault, fieldTypes)

// write documents into an index
val writer = IWriter(index, schema)

val c: PathSet[Path] = Path("corpora", "reuters").children()
c.filter( e => e.name.endsWith(".sgm") ).toList.map(f => articles(f)).flatten.foreach(g => writer.write(document(g)))

writer.close

// search
val searcher = ISearcher(index)
val results = searcher.search(query=new TermQuery(new Term("people", "lawson")), rows=10)

results.foreach(doc => {
  printf("[DocID] %d: %s\n", doc.docId, doc.get("body"))
})
