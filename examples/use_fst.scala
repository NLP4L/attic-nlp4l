import org.nlp4l.core._

val index = "/tmp/index-brown"

// load schema from file
val schema = SchemaLoader.load("examples/schema/brown.conf")

val reader = IReader(index, schema)
val fst = SimpleFST()

reader.field("body_pos").get.terms.foreach { term =>
  fst.addEntry(term.text, term.totalTermFreq)
}

fst.finish

val STR = "iaminnewyork"

for(pos <- 0 to STR.length - 1){
  fst.leftMostSubstring(STR, pos).foreach { e =>
    print("%s".format("             ".substring(0, pos)))
    println("%s => %d".format(STR.substring(pos, e._1), e._2))
  }
}
