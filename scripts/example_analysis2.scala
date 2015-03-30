import org.nlp4l.core.analysis.Analyzer
import org.nlp4l.core.analysis.AnalyzerBuilder
import org.nlp4l.core.analysis.Token

// use of WhitespaceAnalyzer
val wa = Analyzer(new org.apache.lucene.analysis.core.WhitespaceAnalyzer())
wa.tokens("Hello NLP4L").foreach{t: Token => println(t.get("term").get)}

// use of JapaneseAnalyzer
val ja = Analyzer(new org.apache.lucene.analysis.ja.JapaneseAnalyzer())
ja.tokens("こんにちは、NLP4L!").foreach{t: Token => println(t.get("term").get)}
// extract nouns 2 (getOrElseをかけるパターン)
ja.tokens("NLP4LはLuceneのための自然言語処理ツールです。").filter(_.getOrElse("partOfSpeech", "").startsWith("名詞")).map(_.getOrElse("term", ""))

// use of CustomAnalyzer
val builder = AnalyzerBuilder()
builder.withTokenizer("standard")
builder.addTokenFilter("stop")
builder.addTokenFilter("lowerCase")
val ca = builder.build
ca.tokens("NLP4L stands for Natural Language Processing for Lucene.").foreach{t: Token => println(t.get("term").get)}
