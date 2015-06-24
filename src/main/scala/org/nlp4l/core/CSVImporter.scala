/*
 * Copyright 2015 RONDHUIT Co.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nlp4l.core

import java.io.{InputStreamReader, File}
import org.apache.commons.csv._
import org.nlp4l.nee.OpenNLPExtractor

import scala.collection.JavaConversions._
import scala.util.matching.Regex
import scalax.io.Resource

object CSVImporter {

  val usage =
    """
      |Usage:
      |CSVImporter
      |        --index <index dir>
      |        --schema <schema file>
      |        --fields <field names>        e.g. id,title,body
      |        [--enc <encode>]              default is UTF-8
      |        [--format <csv format>]       choose one of standard(default), rfc4180, excel, mysql, or tdf
      |        [--neeModels <model files>]   e.g. en-sent.bin,en-token.bin,en-ner-person.bin
      |        [--neeField <target field>]   name of the target field to be extracted
      |        file1 [file2 ...]
    """.stripMargin

  def required(opts: Map[Symbol, String], key: Symbol): String = {
    val value = opts.getOrElse(key, null)
    if(value == null){
      println("%s must be set.".format(key.name))
      println(usage)
      sys.exit()
    }
    value
  }

  def main(args: Array[String]): Unit = {

    if (args.isEmpty){
      println(usage)
      sys.exit()
    }

    def parseOption(opts: Map[Symbol, String], files: List[String], list: List[String]): (Map[Symbol, String], List[String]) = list match {
      case Nil => (opts, files.reverse)
      case "--index" :: value :: tail => parseOption(opts + ('index -> value), files, tail)
      case "--schema" :: value :: tail => parseOption(opts + ('schema -> value), files, tail)
      case "--fields" :: value :: tail => parseOption(opts + ('fields -> value), files, tail)
      case "--enc" :: value :: tail => parseOption(opts + ('enc -> value), files, tail)
      case "--format" :: value :: tail => parseOption(opts + ('format -> value), files, tail)
      case "--neeModels" :: value :: tail => parseOption(opts + ('neeModels -> value), files, tail)
      case "--neeField" :: value :: tail => parseOption(opts + ('neeField -> value), files, tail)
      case value :: tail => parseOption(opts, value :: files, tail)
    }

    val (opts, files) = parseOption(Map(), List(), args.toList)

    val index = required(opts, 'index)
    val schemaFile = required(opts, 'schema)
    val fields: Array[String] = required(opts, 'fields).split(",")
    val enc = opts.getOrElse('enc, "UTF-8")
    val format = opts.getOrElse('format, "standard")
    val neeModels = opts.getOrElse('neeModels, null)
    val neeField = opts.getOrElse('neeField, null)

    if (files.isEmpty){
      println("No files are passed.")
      println(usage)
      sys.exit()
    }

    files.foreach(f => {
      val file = new File(f)
      if (!file.exists()) throw new IllegalArgumentException("File not found: " + f)
    })

    if((neeModels != null && neeField == null) || (neeModels == null && neeField != null)){
      println("both neeModels and neeField must be specified when using NEE option")
      sys.exit()
    }
    val extractor =
      if(neeField != null){
        val models = neeModels.split(",")
        if(models.size != 3){
          println("specify three models sentence,token,name for OpenNLP")
          sys.exit()
        }
        OpenNLPExtractor(models(0), models(1), models(2))
      }
      else null

    println("Index directory: " + index)
    println("Schema file: " + schemaFile)
    println("Fields: " + fields)
    println("Encoding: " + enc)
    println("CSV format: " + format)
    if(neeField != null){
      println("NEE target field: " + neeField)
      println("NEE model files: " + neeModels)
    }
    println("Files: " + files.mkString(","))

    val csvf: CSVFormat = format.toLowerCase match {
      case "standard" => CSVFormat.DEFAULT
      case "rfc4180" => CSVFormat.RFC4180
      case "excel" => CSVFormat.EXCEL
      case "mysql" => CSVFormat.MYSQL
      case "tdf" => CSVFormat.TDF
      case _ => {
        println("%s is not valid for csv format.".format(format))
        sys.exit()
      }
    }

    val schema = SchemaLoader.loadFile(schemaFile)
    val writer = IWriter(index, schema)
    try{
      writer.deleteAll()
      files.foreach(f => importCsv(writer, enc, f, neeField, extractor, csvf, fields: _*))
    }
    finally{
      writer.close()
    }

    val reader = RawReader(index)
    println("All docs are indexed.")
    println("Num of docs: " + reader.numDocs)
    reader.close
  }

  def importCsv(writer: IWriter, enc: String, file: String, neeField: String, extractor: OpenNLPExtractor,
                csvf: CSVFormat, fields: String*): Unit = {
    val is: java.io.InputStream = Resource.fromFile(file).inputStream.open().get
    val reader: java.io.Reader = new InputStreamReader(is, enc)
    try{
      val parse: CSVParser = csvf.withHeader(fields: _*).parse(reader)
      val records = parse.getRecords()
      records.foreach{ record =>
        writer.write(document(neeField, extractor, record, fields))
      }
    }
    finally{
      reader.close()
      is.close()
    }
  }

  def document(neeField: String, extractor: OpenNLPExtractor, record: CSVRecord, fields: Seq[String]): Document = {
    val flds = fields.map(f => Field(f, record.get(f))).toSet
    if(neeField == null) Document(flds)
    else{
      val entities = extractor.extractNamedEntities(record.get(neeField))
      if(entities.size > 0){
        val map = scala.collection.mutable.Map[String, String]()
        entities.foreach{ entity =>
          val ex = map.getOrElse(entity._2, null)
          if(ex != null){
            map.put(entity._2, ex + "," + entity._1)
          }
          else{
            map.put(entity._2, entity._1)
          }
        }
        val entFields = map.keys.map(k => Field("%s_%s".format(neeField, k), map.get(k).get.split(",")))
        Document(flds ++ entFields)
      }
      else Document(flds)
    }
  }
}
