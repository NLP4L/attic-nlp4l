# NLP4L とは？

NLP4L は Scala で書かれた [Apache Lucene](https://lucene.apache.org/core/) のための自然言語処理ツールです。NLP4L は Lucene インデックスに登録されている文書データを処理対象にしています。そのため、Lucene の強力な Analyzer によって正規化された単語データベースに直接アクセスができるほか、便利な検索機能が使えます。また Scala で書かれているため、会話型でアドホックな処理を試すなども得意としています。

詳しいドキュメントは以下をご覧ください。

- チュートリアル [日本語](http://nlp4l.github.io/tutorial_ja.html)／[English](http://nlp4l.github.io/tutorial.html) (to be available later)
- [API Docs](http://nlp4l.github.io/api/index.html)

# 対象ユーザ

- Lucene/Solr/Elasticsearch など、[Apache Lucene](https://lucene.apache.org/core/) のユーザ
- [Apache Spark](https://spark.apache.org/) や [Apache Mahout](http://mahout.apache.org/) など既存の機械学習ツールへの入力に、さまざまな文書ベクトルを試したいユーザ
- その他の NLPツールユーザや Scala プログラマ

# 簡単な使い方

## NLP4L のビルド

```shell
$ sbt pack
```

## 会話型シェルの起動

```shell
$ target/pack/bin/nlp4l
Welcome to NLP4L!
Type in expressions to have them evaluated.
Type :help for more information
Type :? for information about NLP4L utilities

nlp4l> 
```

## NLP4L のテスト

```shell
$ sbt test
```

# 制限事項

- 最適化が施されていないインデックスでは正確な単語カウントが取得できない可能性があります。
