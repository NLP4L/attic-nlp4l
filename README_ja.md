# NLP4L とは？

NLP4L は Scala で書かれた [Apache Lucene](https://lucene.apache.org/core/) のための自然言語処理ツールです。NLP4L は、NLP 技術を用いて Lucene ユーザの検索エクスペリエンスを向上させることを主な目的としています。たとえば、Lucene/Solr は検索キーワードのオートコンプリートやサジェスチョンの機能などを今でも提供していますが、NLP4L の開発メンバーは NLP 技術を適用することでよりよいキーワードを提示することが可能ではないかと考えています。また、NLP4L は既存の機械学習ツールと連携するための機能も提供します。たとえば、Lucene インデックスから直接文書ベクトルを生成し、LIBSVM 形式のファイルに出力することができます。

NLP4L は Lucene インデックスに登録されている文書データを処理対象にしています。そのため、Lucene の強力な Analyzer によって正規化された単語データベースに直接アクセスができるほか、便利な検索機能が使えます。また Scala で書かれているため、会話型でアドホックな処理を試すなども得意としています。

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

## Lucene インデックスブラウザを使う

### ヘルプ

```shell
nlp4l> :?
```

### 各コマンドのヘルプ

```shell
nlp4l> :? open
-- method signature --
def open(idxDir: String): RawReader

-- description --
Open Lucene index in the directory. If an index already opened, that is closed before the new index will be opened.

-- arguments --
idxDir       Lucene index directory

-- return value --
Return : index reader

-- usage --
nlp4l> open("/tmp/myindex")
```

### インデックスステータスの表示

```shell
nlp4l> open("/tmp/index-ldcc")
Index /tmp/index-ldcc was opened.
res4: org.nlp4l.core.RawReader = IndexReader(path='/tmp/index-ldcc',closed=false)

nlp4l> status

========================================
Index Path       : /tmp/index-ldcc
Closed           : false
Num of Fields    : 5
Num of Docs      : 7367
Num of Max Docs  : 7367
Has Deletions    : false
========================================
        
Fields Info:
========================================
  # | Name  | Num Terms 
----------------------------------------
  0 | body  |      64543
  1 | url   |       7367
  2 | date  |       6753
  3 | title |      14205
  4 | cat   |          9
========================================
```

### 転置インデックスの表示(browseTerms)

```shell
nlp4l> browseTerms("title")
Browse terms for field 'title', page size 20
Type "nextTerms(skip)" or "nt" to browse next terms.
Type "prevTerms(skip)" or "pt" to browse prev terms.
Type "topTerms(n)" to find top n frequent terms.

// nt で次のページ
nlp4l> nt
Indexed terms for field 'title'
0 (DF=152, Total TF=176)
000 (DF=13, Total TF=13)
003 (DF=3, Total TF=3)
0048 (DF=1, Total TF=1)
007 (DF=8, Total TF=8)
...

// pt で前のページに戻る
nlp4l> pt
Indexed terms for field 'title'
chat (DF=1, Total TF=1)
check (DF=2, Total TF=2)
chochokure (DF=1, Total TF=1)
christian (DF=1, Total TF=1)
...
```

### 転置インデックスの表示(browseTermDocss)

```shell
nlp4l> browseTermDocs("title", "iphone")
Browse docs for term 'iphone' in field 'title', page size 20
Type "nextDocs(skip)" or "nd" to browse next terms.
Type "prevDocs(skip)" or "pd" to browse prev terms.

// nd で次のページ
nlp4l> nd
Documents for term 'iphone' in field 'title'
Doc(id=49, freq=1, positions=List(pos=5))
Doc(id=270, freq=1, positions=List(pos=0))
Doc(id=648, freq=1, positions=List(pos=0))
Doc(id=653, freq=1, positions=List(pos=2))
Doc(id=778, freq=1, positions=List(pos=2))
Doc(id=780, freq=2, positions=List(pos=0, pos=15))
...

// pd で前のページに戻る
nlp4l> pd
Documents for term 'iphone' in field 'title'
Doc(id=1173, freq=1, positions=List(pos=1))
Doc(id=1176, freq=1, positions=List(pos=0))
Doc(id=1180, freq=1, positions=List(pos=2))
Doc(id=1195, freq=1, positions=List(pos=5))
Doc(id=1200, freq=1, positions=List(pos=11))
Doc(id=1203, freq=1, positions=List(pos=5))
...
```

### 文書の表示

```shell
nlp4l> showDoc(1195)
Doc #1195
(Field) cat: [it-life-hack]
(Field) url: [http://news.livedoor.com/article/detail/6608703/]
(Field) title: [GoogleドライブのファイルをiPhoneからダイレクトに編集する【知っ得！虎の巻】]
...
```

### インデックスのクローズ

```shell
nlp4l> close
Index /tmp/index-ldcc was closed.
```

## NLP4L のテスト

```shell
$ sbt test
```

# 制限事項

- 最適化が施されていないインデックスでは正確な単語カウントが取得できない可能性があります。
