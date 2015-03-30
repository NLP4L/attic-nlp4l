package org.nlp4l.core.analysis

import org.scalatest.FunSuite

class AnalyzerSuite extends FunSuite with AnalyzerAsserts {
  test("simple StandardAnalyzer test"){
    val a = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer)
    val results = a.tokens("Hello, Lucene!").toArray
    assertToken(results(0), Map("term" -> "hello", "type" -> "<ALPHANUM>", "startOffset" -> "0", "endOffset" -> "5", "positionIncrement" -> "1"))
    assertToken(results(1), Map("term" -> "lucene", "type" -> "<ALPHANUM>", "startOffset" -> "7", "endOffset" -> "13", "positionIncrement" -> "1"))
  }

  test("simple JapaneseAnalyzer test"){
    val a = Analyzer(new org.apache.lucene.analysis.ja.JapaneseAnalyzer)
    val results = a.tokens("NLP4LはLuceneのための自然言語処理ツールです。").toArray
    assertToken(results(0), Map("term" -> "nlp",    "startOffset" -> "0", "endOffset" -> "3", "positionIncrement" -> "1", "partOfSpeech" -> "名詞-一般"))
    assertToken(results(1), Map("term" -> "4",      "startOffset" -> "3", "endOffset" -> "4", "positionIncrement" -> "1", "partOfSpeech" -> "名詞-数"))
    assertToken(results(2), Map("term" -> "l",      "startOffset" -> "4", "endOffset" -> "5", "positionIncrement" -> "1", "partOfSpeech" -> "名詞-一般"))
    assertToken(results(3), Map("term" -> "lucene", "startOffset" -> "6", "endOffset" -> "12", "positionIncrement" -> "2", "partOfSpeech" -> "名詞-固有名詞-組織"))
    assertToken(results(4), Map("term" -> "自然",    "reading" -> "シゼン", "startOffset" -> "16", "endOffset" -> "18", "positionIncrement" -> "4", "partOfSpeech" -> "名詞-形容動詞語幹"))
    assertToken(results(5), Map("term" -> "言語",    "reading" -> "ゲンゴ", "startOffset" -> "18", "endOffset" -> "20", "positionIncrement" -> "1", "partOfSpeech" -> "名詞-一般"))
    assertToken(results(6), Map("term" -> "処理",    "reading" -> "ショリ", "startOffset" -> "20", "endOffset" -> "22", "positionIncrement" -> "1", "partOfSpeech" -> "名詞-サ変接続"))
    assertToken(results(7), Map("term" -> "ツール",  "reading" -> "ツール", "startOffset" -> "22", "endOffset" -> "25", "positionIncrement" -> "1", "partOfSpeech" -> "名詞-一般"))
  }

  test("JapaneseAnalyzer general-noun filter test"){
    val a = Analyzer(new org.apache.lucene.analysis.ja.JapaneseAnalyzer)
    val results = a.tokens("NLP4LはLuceneのための自然言語処理ツールです。")
      .filter(_.getOrElse("partOfSpeech", "").startsWith("名詞-一般")).map(_.getOrElse("term", ""))
    assert(results === List("nlp", "l", "言語", "ツール"))
  }

  test("AnalyzerBuilder test"){
    val builder = AnalyzerBuilder()
    builder.withTokenizer("standard")
    builder.addTokenFilter("stop")
    builder.addTokenFilter("lowerCase")
    val a = builder.build
    val results = a.tokens("NLP4L stands for Natural Language Processing for Lucene.").toArray
    assertToken(results(0), Map("term" -> "nlp4l",      "type" -> "<ALPHANUM>", "startOffset" -> "0",  "endOffset" -> "5",  "positionIncrement" -> "1"))
    assertToken(results(1), Map("term" -> "stands",     "type" -> "<ALPHANUM>", "startOffset" -> "6",  "endOffset" -> "12", "positionIncrement" -> "1"))
    assertToken(results(2), Map("term" -> "natural",    "type" -> "<ALPHANUM>", "startOffset" -> "17", "endOffset" -> "24", "positionIncrement" -> "2"))
    assertToken(results(3), Map("term" -> "language",   "type" -> "<ALPHANUM>", "startOffset" -> "25", "endOffset" -> "33", "positionIncrement" -> "1"))
    assertToken(results(4), Map("term" -> "processing", "type" -> "<ALPHANUM>", "startOffset" -> "34", "endOffset" -> "44", "positionIncrement" -> "1"))
    assertToken(results(5), Map("term" -> "lucene",     "type" -> "<ALPHANUM>", "startOffset" -> "49", "endOffset" -> "55", "positionIncrement" -> "2"))
  }
}
