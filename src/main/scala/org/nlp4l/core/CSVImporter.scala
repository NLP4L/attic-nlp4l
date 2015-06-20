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

import scala.collection.JavaConversions._
import scala.util.matching.Regex
import scalax.io.{Codec, Resource}

object CSVImporter {

  val usage =
    """
      |Usage:
      |CSVImporter
      |        --index <index dir>
      |        --schema <schema file>
      |        --fields <field names>      e.g. id,title,body
      |        [--enc <encode>]            default is UTF-8
      |        [--format <csv format>]     choose one of standard(default), rfc4180, excel, mysql, or tdf
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
      case value :: tail => parseOption(opts, value :: files, tail)
    }

    val (opts, files) = parseOption(Map(), List(), args.toList)

    val index = required(opts, 'index)
    val schemaFile = required(opts, 'schema)
    val fields: Array[String] = required(opts, 'fields).split(",")
    val enc = opts.getOrElse('enc, "UTF-8")
    val format = opts.getOrElse('format, "standard")

    if (args.isEmpty){
      println("No files are passed.")
      println(usage)
      sys.exit()
    }

    files.foreach(f => {
      val file = new File(f)
      if (!file.exists()) throw new IllegalArgumentException("File not found: " + f)
    })

    println("Index directory: " + index)
    println("Schema file: " + schemaFile)
    println("Fields: " + fields)
    println("Encoding: " + enc)
    println("CSV format: " + format)
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
      files.foreach(f => importCsv(writer, enc, f, csvf, fields: _*))
    }
    finally{
      writer.close()
    }

    val reader = RawReader(index)
    println("All docs are indexed.")
    println("Num of docs: " + reader.numDocs)
    reader.close
  }

  def importCsv(writer: IWriter, enc: String, file: String, csvf: CSVFormat, fields: String*): Unit = {
    val is: java.io.InputStream = Resource.fromFile(file).inputStream.open().get
    val reader: java.io.Reader = new InputStreamReader(is, enc)
    try{
      val parse: CSVParser = csvf.withHeader(fields: _*).parse(reader)
      val records = parse.getRecords()
      records.foreach{ record =>
        writer.write(document(record, fields))
      }
    }
    finally{
      reader.close()
      is.close()
    }
  }

  def document(record: CSVRecord, fields: Seq[String]): Document = {
    Document(fields.map(f => Field(f, record.get(f))).toSet)
  }
}
