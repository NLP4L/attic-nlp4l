import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object NLP4LBuild extends Build {
  lazy val root = Project(
      id = "nlp4l",
      base = file("."),
      settings = Defaults.defaultSettings ++ packSettings ++
        Seq(
          version := "0.1-dev",
          scalaVersion := "2.11.6",
          libraryDependencies ++= Seq(
            "org.apache.lucene" % "lucene-core" % "5.0.0",
            "org.apache.lucene" % "lucene-analyzers-common" % "5.0.0",
            "org.apache.lucene" % "lucene-analyzers-kuromoji" % "5.0.0",
            "org.apache.lucene" % "lucene-analyzers-icu" % "5.0.0",
            "org.apache.lucene" % "lucene-queries" % "5.0.0",
            "org.apache.lucene" % "lucene-queryparser" % "5.0.0",
            "org.apache.lucene" % "lucene-suggest" % "5.0.0",
            "org.apache.lucene" % "lucene-codecs" % "5.0.0",
            "org.apache.lucene" % "lucene-backward-codecs" % "5.0.0",
            "org.apache.lucene" % "lucene-misc" % "5.0.0",
            "org.apache.httpcomponents" % "httpclient" % "4.4",
            "com.github.scala-incubator.io" % "scala-io-core_2.11" % "0.4.3",
            "com.github.scala-incubator.io" % "scala-io-file_2.11" % "0.4.3",
            "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
            "junit" % "junit" % "4.12" % "test",
            "com.novocode" % "junit-interface" % "0.11" % "test",
            "org.scala-lang" % "scala-compiler" % "2.11.6",
            "jline" % "jline" % "2.12",
            "com.typesafe" % "config" % "1.2.1",
            "com.jsuereth" % "scala-arm_2.10" % "1.3"
          ),
          testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v"),
          packMain := Map("nlp4l" -> "org.nlp4l.repl.NLP4LMainGenericRunner"),
          packJvmOpts := Map("nlp4l" -> Seq("-Xmx512m", "-Dfile.encoding=UTF-8", "-Dnlp4l.conf=${PROG_HOME}/bin/repl.init")),
          packResourceDir ++= Map(baseDirectory.value / "docs" -> "docs",
            baseDirectory.value / "examples" -> "examples",
            baseDirectory.value / "repl.init" -> "bin/repl.init",
            baseDirectory.value / "target/scala-2.11/api" -> "api"),
          pack <<= pack.dependsOn(doc in Compile)
      )
    )


}
