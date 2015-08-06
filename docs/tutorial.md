### Contents

* [Let's Get Started!](#gettingStarted)
* [Installing NLP4L](#install)
* [Obtaining Practice Corpus](#getCorpora)
    * [NLP4L Interactive Shell](#getCorpora_repl)
    * [What is Index?](#getCorpora_index)
    * [Obtaining livedoor News Corpus and Creating Index](#getCorpora_ldcc)
    * [Creating Index with Data in the Companion CD-ROM that Accompanies the book "言語研究のための統計入門" (ISBN978-4-87424-498-2)](#getCorpora_book)
    * [Obtaining Brown Corpus and Creating an Index](#getCorpora_brown)
    * [Obtaining Reuters Corpus and Creating an Index](#getCorpora_reuters)
    * [Obtaining Wikipedia Data and Creating an Index](#getCorpora_wiki)
    * [NLP4L Schema](#getCorpora_schema)
    * [Importing a CSV File](#getCorpora_csv)
* [Using as NLP Tool](#useNLP)
    * [Counting the Number of Words](#useNLP_wordcounts)
    * [Hidden Markov Model](#useNLP_hmm)
    * [Collocational Analysis Model](#useNLP_collocanalysis)
* [Using Index Browser](#indexBrowser)
    * [Browsing fields and words](#indexBrowser_fields)
    * [Browsing Documents](#indexBrowser_docs)
    * [Position / Offsets](#indexBrowser_posoff)
    * [Extracting the top N words with higher frequency](#indexBrowser_topn)
* [To Solr Users](#dearSolrUsers)
* [To Elasticsearch Users](#dearESUsers)
* [Working with Mahout](#useWithMahout)
* [Working with Spark](#useWithSpark)
* [Using Lucene](#useLucene)
* [Using NLP4L from Apache Zeppelin](#withZeppelin)
    * [Installing Apache Zeppelin](#withZeppelin_install)
    * [Deploying libraries of NLP4L to Apache Zeppelin](#withZeppelin_deploy)
    * [Starting Apache Zeppelin](#withZeppelin_start)
    * [Creating a note and saving NLP4LInterpreter](#withZeppelin_save)
    * [Executing commands or programs of NLP4L](#withZeppelin_exec)
    * [Visualising word counts](#withZeppelin_visualize)
    * [Visualizing Zipf's Law](#withZeppelin_zipfslaw)
* [Developing and Executing NLP4L Programs](#develop)
* [Attribution](#tm)

# Let's Get Started!{#gettingStarted}
# Installing NLP4L{#install}
# Obtaining Practice Corpus{#getCorpora}

Before start analyzing your own text file using NLP4L, you are encouraged to use a practice corpus to check the action. Analyzing your own text file straight away may results in longer time to successfully complete the process or wondering about the ways to evaluate the analysis.

You will be able to actually try the followings and understand them easier if you first create an index using a practice corpus explained here.

Note that the corpora introduced here, except for the livedoor news corpus and wikipedia, are for research purpose only. Please be very careful using them.

## NLP4L Interactive Shell{#getCorpora_repl}

NLP4L has an interactive shell that comes in handy for running commands and Scala codes. Start an interactive shell (REPL) as follows when NLP4L has been built.

```shell
$ ./target/pack/bin/nlp4l
Welcome to NLP4L!
Type in expressions to have them evaluated.
Type :help for more information
Type :? for information about NLP4L utilities
nlp4l>
```

## What is Index?{#getCorpora_index}

NLP4L saves text files, which are put into the natural language process, in the inverted index for Lucene. An inverted index is a file structure that is organized so that you can use a word as the key to obtain a list of document numbers. We will refer to the inverted index simply as "index" in this document.

You can use an NLP4L function to create an index from a text file or otherwise make an existing index created by Apache Solr or Elasticsearch the target of NLP4L process. When you do the latter, however, pay good attention to the version of Lucene that Solr and Elasticsearch are using. Index created using the version that is too old may not sometimes be read by NLP4L Lucene library.

The followings will discuss how to obtain a practice corpus (text file) and create an index from scratch.

## Obtaining livedoor News Corpus and Creating Index{#getCorpora_ldcc}

Execute the following to download and expand a livedoor news corpus from the RONDHUIT site.

```shell
$ mkdir -p ${nlp4l}/corpora/ldcc
$ cd ${nlp4l}/corpora/ldcc
$ wget http://www.rondhuit.com/ download /ldcc-20140209.tar.gz
$ tar xvzf ldcc-20140209.tar.gz
```

Windows users: Please adjust to your own environment as appropriate to execute the scripts.

[Note] NLP4L interactive shell provides commands (Supports Unix-like operating systems only) to execute the above procedures.

```shell
nlp4l> downloadLdcc
Successfully downloaded ldcc-20140209.tar.gz
Try to execute system command: tar xzf /Users/tomoko/repo/NLP4L/corpora/ldcc/ldcc-20140209.tar.gz -C /Users/tomoko/repo/NLP4L/corpora/ldcc
Success.
```

Expanding livedoor news corpus will create subdirectories with the following category names directly under the text directory

```shell
$ ls -l text
total 16
-rw-r--r-- 1 koji staff 223 9 16 2012 CHANGES.txt
-rw-r--r-- 1 koji staff 2182 9 13 2012 README.txt
drwxr-xr-x 873 koji staff 29682 2 9 2014 dokujo-tsushin
drwxr-xr-x 873 koji staff 29682 2 9 2014 it-life-hack
drwxr-xr-x 867 koji staff 29478 2 9 2014 kaden-channel
drwxr-xr-x 514 koji staff 17476 2 9 2014 livedoor-homme
drwxr-xr-x 873 koji staff 29682 2 9 2014 movie-enter
drwxr-xr-x 845 koji staff 28730 2 9 2014 peachy
drwxr-xr-x 873 koji staff 29682 2 9 2014 smax
drwxr-xr-x 903 koji staff 30702 2 9 2014 sports-watch
drwxr-xr-x 773 koji staff 26282 2 9 2014 topic-news
```

In addition, each subdirectory has files where one file contains one article. An article file looks like as follows.

```shell
$ head -n 5 text/sports-watch/sports-watch-6577722.txt
http://news.livedoor.com/article/detail/6577722/
2012-05-21T09:00:00+0900
渦中の香川真司にインタビュー、「ズバリ次のチーム、話を伺いたい」
20日放送、NHK「サンデースポーツ」では、山岸舞彩キャスターが日本代表・香川真司に行ったインタビューの模様を放送した。

```

The first line is the URL, the second line is the date, the third line is the title, and the fourth and the followings are the body of livedoor news article respectively.

Next, we will execute the examples/index_ldcc.scala program from the nlp4l command prompt to add livedoor news corpus to the Lucene index. To do so, start nlp4l as follows and use the load command to execute the examples/index_ldcc.scala program.

```shell
$ bin/nlp4l
nlp4l> :load examples/index_ldcc.scala
```

At the beginning of this program, Lucene index directory created in the manner described later is defined as follows.

```scala
val index = "/tmp/index-ldcc"
```

If this directory does not fit well (such as when you are using Windows), change it to point to another place and use the load command to execute the program again.

The Lucene index directory will look like as follows when this program is executed.

```shell
$ ls -l /tmp/index-ldcc
total 67432
-rw-r--r-- 1 koji wheel 16359884 2 24 13:40 _1.fdt
-rw-r--r-- 1 koji wheel 4963 2 24 13:40 _1.fdx
-rw-r--r-- 1 koji wheel 520 2 24 13:40 _1.fnm
-rw-r--r-- 1 koji wheel 7505 2 24 13:40 _1.nvd
-rw-r--r-- 1 koji wheel 147 2 24 13:40 _1.nvm
-rw-r--r-- 1 koji wheel 453 2 24 13:40 _1.si
-rw-r--r-- 1 koji wheel 11319391 2 24 13:40 _1.tvd
-rw-r--r-- 1 koji wheel 5636 2 24 13:40 _1.tvx
-rw-r--r-- 1 koji wheel 2169767 2 24 13:40 _1_Lucene50_0.doc
-rw-r--r-- 1 koji wheel 3322315 2 24 13:40 _1_Lucene50_0.pos
-rw-r--r-- 1 koji wheel 1272515 2 24 13:40 _1_Lucene50_0.tim
-rw-r--r-- 1 koji wheel 26263 2 24 13:40 _1_Lucene50_0.tip
-rw-r--r-- 1 koji wheel 136 2 24 13:40 segments_1
-rw-r--r-- 1 koji wheel 0 2 24 13:40 write.lock
```

## Creating Index with Data in the Companion CD-ROM that Accompanies the book "言語研究のための統計入門" (ISBN978-4-87424-498-2){#getCorpora_book}

If you have the following book [1], you can use the data in CD-ROM that accompanies this book as your corpus. Otherwise, please proceed to the next section.

```shell
[1] 言語研究のための統計入門
石川慎一郎、前田忠彦、山崎誠 編
くろしお出版
ISBN978-4-87424-498-2
```

Copy folders under "INDIVIDUAL WRITERS" (excluding "INDIVIDUAL WRITERS") and folders under "PLAIN"(including "PLAIN") in the data of CD-ROM that accompanies the book "言語研究のための統計入門" to corpora/CEEAUS. When copying is complete, check to see if it looks like the followings.

```shell
# Creating a Directory
$ mkdir -p corpora/CEEAUS

# Copy the CD-ROM

# Confirm the Copied Contents
$ find corpora/CEEAUS -type d
corpora/CEEAUS
corpora/CEEAUS/CEECUS
corpora/CEEAUS/CEEJUS
corpora/CEEAUS/CEENAS
corpora/CEEAUS/CJEJUS
corpora/CEEAUS/PLAIN
```

The CEEAUS corpus is characterized by its highly controlled writing condition. There are 2 articles with respective themes; one is about "Part-time jobs of college students" (file names with "ptj") and the other is "Complete nonsmoking in restaurants" (file names with "smk"). Subdirectories under CEEAUS are divided as follows.

|Subdirectories|Contents|
|:----:|:----------------------------|
|CEEJUS|日本人大学生による英作文770本(770 English essays by Japanese college students)|
|CEECUS|中国人大学生による英作文92本(92 English essays by Chinese college students)|
|CEENAS|成人 English 母語話者による英作文92本(92 English essays by adult native speakers)|
|CJEJUS|日本人大学生による日本語作文50本(50 Japanese essays by Japanese college students)|
|PLAIN|上記すべてを含む(All of the above)|

Now we will create a Lucene index from the CEEAUS corpus. First, use corpora other than PLAIN to create a Lucene index.

```shell
$ bin/nlp4l
nlp4l> :load examples/index_ceeaus.scala
```

Next, use PLAIN to create a Lucene index.

```shell
$ bin/nlp4l
nlp4l> :load examples/index_ceeaus_all.scala
```

As you can see by looking at the beginning of each program, each Lucene indexes are created in /tmp/index-ceeaus and /tmp/index-ceeaus-all respectively. As in the previous example, you need to rewrite the lines and execute the program again if you want to create them in other directories.

## Obtaining Brown Corpus and Creating an Index{#getCorpora_brown}

Download the Brown corpus to corpora/brown and expand it as follows.

```shell
$ mkdir ${nlp4l}/corpora/brown
$ cd ${nlp4l}/corpora/brown
$ wget https://ia600503.us.archive.org/21/items/BrownCorpus/brown.zip
$ unzip brown.zip
```

Windows users: Please adjust to your own environment as appropriate to execute the scripts.

[Note] NLP4L interactive shell provides commands (Supports Unix-like operating systems only) to execute the above procedures.

```shell
nlp4l> downloadBrown
Successfully downloaded brown.zip
Try to execute system command: unzip -o /Users/tomoko/repo/NLP4L/corpora/brown/brown.zip -d /Users/tomoko/repo/NLP4L/corpora/brown
Success.
```

Now, create the Lucene index at the nlp4l prompt.

```shell
$ bin/nlp4l
nlp4l> :load examples/index_brown.scala
```

As you can see by looking at the beginning of each program, the Lucene index is created in /tmp/index-brown. As in the previous example, you need to rewrite the lines and execute the program again if you want to create it in other directories.


## Obtaining Reuters Corpus and Creating an Index{#getCorpora_reuters}

You can obtain the Reuters corpus by [applying to](http://trec.nist.gov/data/reuters/reuters.html) NIST (National Institute of Standards and Technology). Here we will use an archive that you can download from [Dr. David D. Lewis's site](http://www.daviddlewis.com/resources/testcollections/rcv1/) introduced in the NIST site in order to show you how to create indexes for your reference.

Download the Reuters corpus to corpora/reuters and expand it as follows.


```shell
$ mkdir ${nlp4l}/corpora/reuters
$ cd ${nlp4l}/corpora/reuters
$ wget http://www.daviddlewis.com/resources/testcollections/reuters21578/reuters21578.tar.gz
$ tar xvzf reuters21578.tar.gz
```

Windows users: Please adjust to your own environment as appropriate to execute the scripts.

[Note] NLP4L interactive shell provides commands (Supports Unix-like operating systems only) to execute the above procedures.

```shell
nlp4l> downloadReuters
Successfully downloaded reuters21578.tar.gz
Try to execute system command: tar xzf /Users/tomoko/repo/NLP4L/corpora/reuters/reuters21578.tar.gz -C /Users/tomoko/repo/NLP4L/corpora/reuters
Success.
```

Create the Lucene index at the nlp4l prompt.

```shell
$ bin/nlp4l
nlp4l> :load examples/index_reuters.scala
```

Looking at the program in the same manner as before, you can see that Lucene index is created in /tmp/index-reuters. As in the previous example, you need to rewrite the lines and execute the program again if you want to create it in other directories.

## Obtaining Wikipedia Data and Creating an Index{#getCorpora_wiki}

Wikipedia data is one of the most highly favored corpora for NLP study. However, as Wikipedia articles are written by the rule unique to Wikipedia, you need to make an effort to run preprocess to extract only the text data before loading it to an Lucene index. This additional effort might be a hurdle that you have to overcome when it comes to using Wikipedia data.

Now, we will show how to use [json-wikipedia](https://github.com/diegoceccarelli/json-wikipedia) to quickly load Wikipedia data into a Lucene index.

### Downloading and building json-wikipedia

First, create a work directory called "work" [json-wikipedia](https://github.com/diegoceccarelli/json-wikipedia) and download json-wikipedia in that directory.

```shell
$ mkdir work
$ cd work
$ wget https://github.com/diegoceccarelli/json-wikipedia/archive/master.zip
$ unzip master.zip
```

Next, build json-wikipedia in the directory that is created when you unzip and expand the file.

```shell
$ cd json-wikipedia-master
$ mvn assembly:assembly
```

The JAR file created in the target directory will be referenced from NLP4L when you add Wikipedia data, which is converted to JSON, to a Lucene index.

```shell
$ ls target
archive-tmp json-wikipedia-1.0.0.jar
classes maven-archiver
generated-sources surefire-reports
generated-test-sources test-classes
json-wikipedia-1.0.0-jar-with-dependencies.jar
```

### Downloading Wikipedia Data and Converting to JSON

Go to the respective links of languages on the [Wikipedia download site](https://dumps.wikimedia.org/backup-index.html) - jawiki for Japanese, enwiki for English and so forth. Then, download and expand a file labeled XXwiki-YYYYMMDD-pages-articles.xml.bz2 (where XX is language and YYYYMMDD is date).

```shell
$ wget https://dumps.wikimedia.org/jawiki/20150512/jawiki-20150512-pages-articles.xml.bz2
$ bunzip2 jawiki-20150512-pages-articles.xml.bz2
```

Then, execute json-wikipedia as follows to convert it to the JSON format.

```shell
$ ./scripts/convert-xml-dump-to-json.sh en jawiki-20150512-pages-articles.xml /tmp/jawiki.json
```

Specify language in the first argument. For now, json-wikipedia supports very limited number of languages, including English (en) and Italian (it),  and does not support Japanese. The above example, therefore, specifies English (en) in the first argument (it seems to work without any problem). It takes nearly 30 minutes to convert Japanese wikipedia to the JSON format.

### Creating Lucene Index

Finally, we will add data in the JSON format from NLP4L to the Lucene index. Note that the json-wikipedia JAR file built from this procedure needs to be included in the NLP4L class path.

```shell
$ ./target/pack/bin/nlp4l -cp json-wikipedia-1.0.0-jar-with-dependencies.jar 
nlp4l> 
```

Only you have to do now is to execute examples/index_jawiki.scala as follows. You, however, have to duplicate this sample program and write other programs for other languages as this is a program for Japanese Wikipedia. What requires the most special attention is the schema setting file that the program refer to. Examples/schema/jawiki.conf specifies JapaneseAnalyzer as it processes Japanese. You might want to use StandardAnalyzer for other languages including English where words are separated by spaces.

```shell
nlp4l> :load examples/index_jawiki.scala
```

The Lucene index will be created in about 30 minutes for the Japanese Wikipedia.

## NLP4L Schema{#getCorpora_schema}

The Lucene index is basically schemaless but NLP4L can set up a schema. Among the practice corpora, now let's look at schemas that are defined by the livedoor news corpus (ldcc) , CEEAUS, or the Brown corpus (brown).

The table below lists field names that each corpus has (x specifies that the field is available in the corresponding corpus).

| Field Names|ldcc|CEEAUS|brown|
|:----------:|:--:|:----:|:---:|
|file | | x | x |
|type | | x | |
|cat | x | x | x |
|url | x | | |
|date | x | | |
|title | x | | |
|body | x | | x |
|body_en | | x | |
|body_ws | | x | |
|body_ja | | x | |
|body_pos | | | x |

In the title and the following fields, Lucene Analyzer divides texts by word when corpora are added. Lucene Analyzer is sophisticated, supporting not only division but also normalization including stop word extraction, stemming, and variable character conversion. We will discuss how it performs division/normalization later.

Words are not divided in other fields when they are added, and the character strings of corpus are added as they are. Especially, as the cat field has document categories, it can be used in tasks including the document classification.

### ldcc Schema

Title holds the title of news article while body holds the body of article. Lucene's JapaneseAnalyzer divides words in the both fields.

### CEEAUS Schema

Type holds the CEEJUS/CEECUS/CEENAS/CJEJUS type while cat holds ptj (part-time jobs of college students) /smk (complete nonsmoking in restaurants) category. Though body_en and body_ws hold the same sentence, Lucene StandardAnalyzer is applied to body_en while Lucene WhitespaceAnalyzer is applied to body_ws. These fields are used according to their purposes; the hypothesis test below uses body_en while correlation analysis uses body_ws. Body_ja holds Japanese text from the CJEJUS subcorpus. Lucene JapaneseAnalyzer is applied to this field.

### Brown Schema

Each article in the Brown corpus has the word class tags attached by words as follows. 

```shell
The/at Fulton/np-tl County/nn-tl Grand/jj-tl Jury/nn-tl said/vbd Friday/nr an/at investigation/nn of/in Atlanta's/np$ recent/jj primary/nn election/nn produced/vbd ``/`` no/at evidence/nn ''/'' that/cs any/dti irregularities/nns took/vbd place/nn ./.
```

The body_pos field holds this text as it is while the body field holds the same contents but has its word class tags removed.



## Importing a CSV File{#getCorpora_csv}

We have been using practice corpus to create Lucene indexes. Now, let's look at how to import an original CSV file to a Lucene index.

As an example, let's assume that we have a following CSV file.

```shell
$ cat << EOF > /tmp/data.csv
1, NLP4L, "NLP4L is a natural language processing tool for Apache Lucene written in Scala."
2, NLP4L, "The main purpose of NLP4L is to use the NLP technology to improve Lucene users' search experience."
3, LUCENE, "Apache Lucene is a high-performance, full-featured text search engine library written entirely in Java."
4, SOLR, "Solr is highly reliable, scalable and fault tolerant, providing distributed indexing, replication and load-balanced querying, automated failover and recovery, centralized configuration and more."
5, SOLR, "Solr powers the search and navigation features of many of the world's largest internet sites."
EOF
```

Now, suppose our schema file is as follows.

```shell
$ cat << EOF > /tmp/schema.conf
schema {
 defAnalyzer {
   class : org.apache.lucene.analysis.standard.StandardAnalyzer
 }
 fields　= [
   {
     name : id
     indexed : true
     stored : true
   }
   {
     name : cat
     indexed : true
     stored : true
   }
   {
     name : body
     analyzer : {
       tokenizer {
         factory : standard
	}
       filters = [
         {
           factory : lowercase
         }
       ]
     }
     indexed : true
     stored : true
     termVector : true
     positions : true
     offsets : true
   }
 ]
}
EOF
```

Then, run the command to import CSV file as follows.

```shell
$ java -cp "target/pack/lib/*" org.nlp4l.core.CSVImporter --index /tmp/index-tmp --schema /tmp/schema.conf --fields id,cat,body /tmp/data.csv
```

# Using as NLP Tool{#useNLP}

We will discuss how to use NLP4L as an NLP tool. Please prepare at hand a practice corpus registered in the Lucene index discussed above so you can use it anytime.

## Counting the Number of Words{#useNLP_wordcounts}

Counting the number of words that appear in the corpus is one of NLP processing fundamentals. NLP4L registers corpus to a Lucene index before processing and is very good at counting the number of words as a search engine (Lucene) has something called an inverted index that uses words as keys.

Now we will discuss how to use a Reuters corpus to find out the frequency that words appear. As a starter, copy the following program, paste it to the nlp4l prompt and run it. Note that here we omitted the nlp4l prompt for a program that extends to more than one line so you can easily handle copy and paste.

```scala
// (1)
import org.nlp4l.core._
import org.nlp4l.core.analysis._
import org.nlp4l.stats.WordCounts

// (2)
val index = "/tmp/index-reuters"

// (3)
val reader = RawReader(index)
```

(1) imports necessary Scala program packages where the WordCounts object is used for the word frequency . (2) specifies the Lucene index directory of Reuters corpus while (3) specifies the Lucene index directory to be used for RawReader in order to obtain reader. RawReader, in NLP4L, is a comparatively low level Reader. There also is a high level Reader called IReader that manages schema. We, however, will specifically use RawReader in order to avoid the trouble of passing a schema.

Using the obtained reader, we will count the word frequency in the following. As Lucene has independent inverted index in every field, you need to specify a field name to perform such processes as counting number of words. In the following, we will specify a body field that has the entire body of Reuters story.

### Total Word Count and Unique Word Count 

We will first discuss the total word count. Lucene originally has a function to return the total word count of an unspecified field. You can, therefore, easily find out the number by using the sumTotalTermFreq() function that is the Scala wrapper of this function.

```shell
nlp4l> val total = reader.sumTotalTermFreq("body")
total: Long = 1899819
```

The next is a unique word count that is the number of word types. The unique word count equals to the size of inverted index as it has a structure that uses unique words as keys. Lucene, of course, can easily check it. Its Scala wrapper will be something like follows.

```shell
nlp4l> val count = reader.field("body").get.terms.size
count: Int = 64625
```

### Counting by Words

Next, we will be more specific and try counting by words. You can count by words using the count() function of WordCounts object. However, you need some preparation because the count() function takes more than one argument. Refer to the following program - you can copy and paste the program and run it at the nlp4l prompt.

```scala
// (4)
val allDS = reader.universalset()

// (5)
val analyzer = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer(null.asInstanceOf[org.apache.lucene.analysis.util.CharArraySet]))

// (6)
val allMap = WordCounts.count(reader, "body", Set.empty, allDS, -1, analyzer)
```

(4) obtains a target Lucene document number that is used by the count() function. We use the universalset() function, which obtains the total set of a document, as we target the all documents here. (5) then specifies StandardAnalyzer, a standard Analyzer for Lucene, to create the Scala Analyzer. The null, which is specified as an argument for StandardAnalyzer, specifies that you do not use any stop words (the default stop words for StandardAnalyzer will be used when null is not specified). The count() function in (6) calculates word frequency for every word. Set.empty that is specified as an argument specifies that "the all words" will be the target of count. "-1" specifies the number of words with most frequency from the top to "-1". Specifying -1 means that the target will be the all words.

Now the result is displayed but is hard to read in the way it is displayed now. We, therefore, limit the number of data that is displayed. For example, using Scala collection function to prepare the following will enable you to display 10 words that start with "g" and their frequency.

```shell
nlp4l> allMap.filter(_._1.startsWith("g")).take(10).foreach(println(_))
(generalize,1)
(gress,1)
(germans,18)
(goiporia,2)
(garcin,2)
(granma,7)
(gorbachev's,10)
(gamble,9)
(grains,110)
(gienow,1)
```

Add the all numbers resulting from allMap - the following program basically performs the same function as the totalCount() function.

```shell
nlp4l> allMap.values.sum
res2: Long = 1899819
```

This certainly matches the total word count that we first found out. Also, the size of allMap should match the unique word count. Let's find out.

```shell
nlp4l> allMap.size
res3: Int = 64625
```

Here we have another match. 

The above count() passed Set.empty to target the all words in order to count the advent of frequency. You can also pass a specific word set instead of Set.empty to count only this word set - in NLP, you often want to count only the specific words. Let's find out.

```scala
val whWords = Set("when", "where", "who", "what", "which", "why")
val whMap = WordCounts.count(reader, "body", whWords, allDS, -1, analyzer)
whMap.foreach(println(_))
```

The result will be as follows.

```shell
nlp4l> whMap.foreach(println(_))
(why,125)
(what,850)
(who,1618)
(which,7556)
(where,507)
(when,1986)
```

### Counting by Category

Next, let's look at how to find out the word frequency by category. For example, document classification, one of an NLP tasks, sometimes uses word frequency by category as its learning data in order to classify by category. This is the technique that you can use in such cases.

Here we use a sample Reuters corpus but will try the places field instead of category. To do so, we first find the words in the places field as follows.

```scala
// (7)
reader.terms("places").get.map(_.text)

// (8) When you want to fomat before displaying.
reader.terms("places").get.map(_.text).foreach(println(_))
```

Running (7) or (8) will display the list of all words registered in the places field. Let's focus on usa and japan here. Run the program as shown in (9) to obtain respective document subsets.

```scala
// (9)
val usDS = reader.subset(TermFilter("places", "usa"))
val jpDS = reader.subset(TermFilter("places", "japan"))
```

Finally, pass the respective subsets obtained in (9) to the count() function to obtain the usa count and the japan count. In (10), however, we will quickly obtain the counts for two words: war and peace.

```shell
// (10)
nlp4l> WordCounts.count(reader, "body", Set("war", "peace"), usDS, -1, analyzer)
res22: Map[String,Long] = Map(war -> 199, peace -> 14)

nlp4l> WordCounts.count(reader, "body", Set("war", "peace"), jpDS, -1, analyzer)
res23: Map[String,Long] = Map(war -> 75, peace -> 2)
```

Now we obtained counts for 2 words, war and peace, with place one for usa and the other for japan. Good! Or is it?

The fact is that the places field may have more than one name of place. In sum, an article of usa and that of japan may have some redundancy. Let's find out. As usDS and jpDS are Set collection objects of Scala, you can use the & operator (function) to easily obtain product sets for the both.

```shell
nlp4l> (usDS & jpDS).size
res24: Int = 452
```

Here we use size to obtain the size of product set. Now we can see there is a redundancy. In this case, you can use &\~ of Scala Set operator (function) as specified in (11) and (12) to get difference of sets to obtain word frequency for the portion that has no redundancy. Note that toSet is used here to do a conversion to Set because SortedSet does not have &\~ operator (function).

```shell
nlp4l> // (11) Articles where places has a value usa but does not have a value japan will be the target.
nlp4l> WordCounts.count(reader, "body", Set("war", "peace"), usDS.toSet &~ jpDS.toSet, -1, analyzer)
res25: Map[String,Long] = Map(war -> 140, peace -> 13)

nlp4l> // (12) Articles where places has a value japan but does not have a value usa will be the target. 
nlp4l> WordCounts.count(reader, "body", Set("war", "peace"), jpDS.toSet &~ usDS.toSet, -1, analyzer)
res26: Map[String,Long] = Map(war -> 16, peace -> 1)
```

## Hidden Markov Model {#useNLP_hmm}

NLP4L provides the HmmModel class that you can use to learn Hidden Markov model from labeled training data. The both HmmModel and HmmModelIndexer refer to a Lucene index schema that is defined by the next HmmModelSchema.

```scala
trait HmmModelSchema {
 def schema(): Schema = {
  val analyzer = Analyzer(new org.apache.lucene.analysis.core.WhitespaceAnalyzer)
  val builder = AnalyzerBuilder()
  builder.withTokenizer("whitespace")
  builder.addTokenFilter("shingle", "minShingleSize", "2", "maxShingleSize", "2", "outputUnigrams", "false")
  val analyzer2g = builder.build
  val fieldTypes = Map(
   "begin" -> FieldType(analyzer, true, true, true, true),
   "class" -> FieldType(analyzer, true, true, true, true),
   "class_2g" -> FieldType(analyzer2g, true, true, true, true),
   "word_class" -> FieldType(analyzer, true, true, true, true),
   "word" -> FieldType(analyzer, true, true, true, true)
  )
  val analyzerDefault = analyzer
  Schema(analyzerDefault, fieldTypes)
 }
}
```

The Hidden Markov model uses the probability of transition from one status (class) to another and the probability of symbol (word) output at one status. NLP4L uses a Lucene index that has a schema defined by HmmModelSchema to obtain these probabilities. The status transition probability uses the class frequencies of the class and the class_2g fields. The class_2g is a field that uses ShingleFilter of Lucene to store the 2-gram class. The class field, on the other hand, is a field that simply stores classes. Let's consider using these 2 fields to calculate the probability P( vb | nn ): that is the probability of a part of speech vb appears after a part of speech nn. This actually is extremely easy to do as you can use the previously described totalTermFreq(), which calculates the number of words, to calculate as follows.

```math
P( vb | nn ) = "nn vb".totalTermFreq() / "nn".totalTermFreq()
```

A series of 2 classes such as "nn vb" refers to the class_2g field while "nn" refers to the class field. Similarly, the output probability of a word "program" in the class nn can be calculated as follows.

```math
P( program | nn ) = "program_nn".totalTermFreq() / "nn".totalTermFreq()
```

Here, "program_nn" is a character string added in the Lucene index when a word "program" and a clas nn are observed at the same time. This is stored in the word_class field.

The first class in the Lucene documents is stored in the begin field. You can calculate the initial status probability distribution of each class if you use the totalTermFreq() of this field. 

Let's look at specific examples of HmmModel usage.

### Part of Speech Tagging

One of the popular success examples of Hidden Markov model application is the part of speech tagging. Since the English sentences in the brown corpus are part of speech tagged, you can learn HmmModel from these and tag unknown English sentences with part of speeches. First, prepare brown corpus and run the sample script as follows.

```shell
nlp4l> :load examples/hmm_postagger.scala
```

The program, at the end, uses the Lucene index model, which has completed learning, to perform part of speech tagging on unknown English sentences. For example, performing part of speech tagging on an English sentence "i like to go to france ." will produce the following result.

```scala
res8: Seq[org.nlp4l.lm.Token] = List(Token(i,ppss), Token(like,vb), Token(to,to), Token(go,vb), Token(to,in), Token(france,np), Token(.,.))
```

Let's look at the learning portion at the begenning of sample program.

```scala
// (1)
val index = "/tmp/index-brown-hmm"

// (2)
val c: PathSet[Path] = Path("corpora", "brown", "brown").children()

// (3)
val indexer = HmmModelIndexer(index)
c.filter{ e =>
 val s = e.name
 val c = s.charAt(s.length - 1)
 c >= '0' && c <= '9'
}.foreach{ f =>
 val source = Source.fromFile(f.path, "UTF-8")
 source.getLines().map(_.trim).filter(_.length > 0).foreach { g =>
  val pairs = g.split("\\s+")
  val doc = pairs.map{h => h.split("/")}.filter{_.length==2}.map{i => (i(0).toLowerCase(), i(1))}
  indexer.addDocument(doc)
 }
}

// (4)
indexer.close()

// (5)
val model = HmmModel(index)
```

The program first specifies the Lucene index that the brown corpus is stored at (1). (2) specifies the directory for the brown corpus. This is referred to at (3) in order to get a file one at a time. The program then creates HmmModelIndexer at (3) and considers a line in a brown corpus file as one of Lucene documents and adds it to HmmModelIndexer using addDocument(). The format of documents that will be added is a double sequence of word and part of speech.

Once a Lucene index is created, the program closes it at (4). The Hidden Markov model is calculated when HmmModel reads Lucene index, which was created like the above, at (5).

Apply a model to HmmTagger as follows and call tokens() of HmmTagger when you use a model for English part of speech tagging.

```scala
val tagger = HmmTagger(model)

tagger.tokens("i like to go to france .")
tagger.tokens("you executed lucene program .")
tagger.tokens("nlp4l development members may be able to present better keywords .")
```

The result of execution will be returned as follows in the form of a Token object list that has word and class (part of speech) as its elements.

```shell
res23: Seq[org.nlp4l.lm.Token] = List(Token(i,ppss), Token(like,vb), Token(to,to), Token(go,vb), Token(to,in), Token(france,np), Token(.,.))
res24: Seq[org.nlp4l.lm.Token] = List(Token(you,ppo-tl), Token(executed,vbn), Token(lucene,X), Token(program,nil), Token(.,.))
res25: Seq[org.nlp4l.lm.Token] = List(Token(nlp4l,X), Token(development,nn), Token(members,nns), Token(may,md), Token(be,be), Token(able,jj), Token(to,to), Token(present,vb), Token(better,rbr), Token(keywords,X), Token(.,.-hl))
```

### Estimating English Words from Katakana Words

NLP4L includes training data that has pairs of Katakana words and their originating English words with original alignments added (train_data/alpha_katakana_aligned.txt).

```shell
$ head train_data/alpha_katakana_aligned.txt 
アaカcaデdeミーmy
アaクcセceンnトt
アaクcセceスss
アaクcシciデdeンnトt
アaクcロroバッbaトt
アaクcショtioンn
アaダdaプpターter
アaフfリriカca
エaアirバbuスs
アaラlaスsカka
```

Let's think about learning Hidden Markov model, using this data and considering Katakana portion as words and alphabet portion as part of speeches. In this way, you will be able to predict English words from unknown Katakana words. This is implemented in examples/trans_katakana_alpha.scala. 

```shell
nlp4l> :load examples/trans_katakana_alpha.scala
```

If you are interested, please figure this code out as this is practically the same as the previous examples/hmm_postagger.scala. 

The program at the end uses learned model to display the resulting English words that it estimated from unknown Katakana words.

```scala
val tokenizer = HmmTokenizer(model)

tokenizer.tokens("アクション")
tokenizer.tokens("プログラム")
tokenizer.tokens("ポイント")
tokenizer.tokens("テキスト")
tokenizer.tokens("コミュニケーション")
tokenizer.tokens("エントリー")
```

This result of execution will look like as follows.

```shell
res35: Seq[org.nlp4l.lm.Token] = List(Token(ア,a), Token(ク,c), Token(ショ,tio), Token(ン,n))
res36: Seq[org.nlp4l.lm.Token] = List(Token(プ,p), Token(ロ,ro), Token(グ,g), Token(ラ,ra), Token(ム,m))
res37: Seq[org.nlp4l.lm.Token] = List(Token(ポ,po), Token(イ,i), Token(ン,n), Token(ト,t))
res38: Seq[org.nlp4l.lm.Token] = List(Token(テ,te), Token(キス,x), Token(ト,t))
res39: Seq[org.nlp4l.lm.Token] = List(Token(コ,co), Token(ミュ,mmu), Token(ニ,ni), Token(ケー,ca), Token(ショ,tio), Token(ン,n))
res40: Seq[org.nlp4l.lm.Token] = List(Token(エ,e), Token(ン,n), Token(ト,t), Token(リー,ree))
```

### Extracting Pairs of Katakana and English Words from Japanese wikipedia {#useNLP_hmm_lw}

The preceding examples/trans_katakana_alpha.scala is a program that takes Katakana words and outputs English words. The output English words, however, may be correct or incorrect as they purely are results of estimation. Therefore, they cannot be used in the synonym dictionary for Lucene/Solr as they are. However, when you take a considerable number of documents and extract pairs of Katakana words and alphabet character strings that appear within close range, these estimates could be used to see if each pair has the same meaning. It would be safe to consider that if an English word estimated from an obtained Katakana word is similar to an alphabet character string, the obtained Katakana word and the alphabet character string have the same meaning and can be used as an entry in the synonym dictionary.

A Lucene index /tmp/index-jawiki, which was created from Japanese wikipedia by [Obtaining wikipedia Data and Creating an Index](#getCorpora_wiki), has a field ka_pair defined and this field contains pairs of Katakana words and alphabet character strings that appeared within close range. You will be able to extract pairs of Katakana words and their words origin - alphabet character strings - if you run the program LoanWordsExtractor with this field as a target.

```shell
$ java -Dfile.encoding=UTF-8 -cp "target/pack/lib/*" org.nlp4l.syn.LoanWordsExtractor --index /tmp/index-jawiki --field ka_pair loanwords.txt
```

Also, run SynonymRecordsUnifier as follows because running this program only might result in some redundant lines.

```shell
$ java -Dfile.encoding=UTF-8 -cp "target/pack/lib/*" org.nlp4l.syn.SynonymRecordsUnifier loanwords.txt
```

The ultimately obtained file loanwords.txt_checked can be used as the synonym dictionary for Lucene/Solr.

## Collocational Analysis Model {#useNLP_collocanalysis}

NLP4L can look at a word in corpus and analyze the words before or after it with frequency of occurrence. NLP4L named this analysis data model "collocational analysis model" (CollocationalAnalysisModel). Using collocational analysis model will find out a verb and a preposition or other part of speeches that likely to appear with the verb. For example, English learners could obtain commonly used wordings if they use the collocational analysis model to check English corpus.

Examples/colloc_analysis_brown.scala analyzes the brown corpus and displays words that likely to appear before or after word "found" in the order of appearance. The following is an easy-to-see table of examples/colloc_analysis_brown.scala output.

| 3rd Prev. | 2nd Prev. | Previous | Watched  | Following | 2nd Fol. | 3rd Fol. |
|:------------:|:------------:|:------------:|:--------:|:------------:|:------------:|:------------:|
|    the(36)|    to(26)|    be(60)|   found|    in(77)|    the(46)|    the(28)|
|    and(13)|    he(20)|    he(50)|     |     a(43)|    in(24)|    to(19)|
|     a(12)|    the(17)|    was(32)|     |   that(42)|     a(13)|    in(15)|
|    of(11)|    and(15)|    and(26)|     |    the(38)|    be(13)|    of(13)|
|    is( 9)|   have(13)|   been(26)|     |    it(28)|    to(12)|    and( 9)|
|    was( 8)|    can(10)|     i(26)|     |    to(28)|    and(11)|     a( 8)|
|    he( 7)|     i(10)|    she(23)|     |  himself(13)|    of(11)|   with( 8)|
|    in( 7)|    has( 9)|   have(22)|     |    out(12)|    he( 7)|    had( 7)|
|   that( 7)|    of( 9)|    had(21)|     |    him( 9)|    at( 6)|    be( 6)|
|    as( 5)|   could( 8)|   they(19)|     |  myself( 9)|    had( 5)|    men( 4)|

You can see that there are a lot of phrases "found in" here. Also, as you can look at words before the closely watched word "found", you can see that there are possibilities that phrases "be found", "to be found", and "can be found" are appearing as well.

# Using Index Browser{#indexBrowser}

The interactive shell of NLP4L includes the CUI Lucene index browser. Using this index browser enables you to quickly browse/debug the information on fields and words in the Lucene index without writing a Java program.

Now, let's browse inside of Livedoor news corpus that we created in the preceding section.

Note that you can list the index browser commands (methods) by typing ":?" at the nlp4l prompt.

```shell
nlp4l> :?
```

Also, adding a command after ":?" displays more detailed help contents.

```shell
nlp4l> :? open
-- method signature --
def open(idxDir: String): RawReader

-- description --
Open Lucene index in the directory. If an index already opened, that is closed before the new index will be opened.

-- arguments --
idxDir    Lucene index directory

-- return value --
Return : index reader

-- usage --
nlp4l> open("/tmp/myindex")
```

## Browsing fields and words {#indexBrowser_fields}

Pass an index directory path to the open() function to open the index.

```shell
nlp4l> open("/tmp/index-ldcc")
Index /tmp/index-ldcc was opened.
res4: org.nlp4l.core.RawReader = IndexReader(path='/tmp/index-ldcc',closed=false)
```

The close command closes the currently open index.

```shell
nlp4l> close
Index /tmp/index-ldcc was closed.
```

Typing status will display an overview of the currently open index including the number of documents and field information as well as unique word count of each field.

```shell
nlp4l> open("/tmp/index-ldcc")
Index /tmp/index-ldcc was opened.
res4: org.nlp4l.core.RawReader = IndexReader(path='/tmp/index-ldcc',closed=false)

nlp4l> status

========================================
Index Path    : /tmp/index-ldcc
Closed      : false
Num of Fields  : 5
Num of Docs   : 7367
Num of Max Docs : 7367
Has Deletions  : false
========================================
    
Fields Info:
========================================
 # | Name | Num Terms 
----------------------------------------
 0 | body |   64543
 1 | url  |    7367
 2 | date |    6753
 3 | title |   14205
 4 | cat  |     9
========================================
```

In addition, passing a field name to the browseTerms() function enables you to use nextTerms and prevTerms functions to browse word information in the field.

You can pass page numbers you want to skip to nextTerms() and prevTerms(). Also, nextTerms(1) and prevTerms(1) define shortcut functions pt and nt respectively.

Now, let's browse words in the title field. The words are in the dictionary order. The beginning of each line is an indexed word, DF is the number of documents that includes the word (document frequency), and Total TF (term frequency) is the total occurrences of that word.

```shell
nlp4l> browseTerms("title")
Browse terms for field 'title', page size 20
Type "nextTerms(skip)" or "nt" to browse next terms.
Type "prevTerms(skip)" or "pt" to browse prev terms.
Type "topTerms(n)" to find top n frequent terms.

// nt to go to the next page.
nlp4l> nt
Indexed terms for field 'title'
0 (DF=152, Total TF=176)
000 (DF=13, Total TF=13)
003 (DF=3, Total TF=3)
0048 (DF=1, Total TF=1)
007 (DF=8, Total TF=8)
...

// Move a page a few times or use nextTerms(n) to skip a line or a few will show you words that start with an alphabet.
nlp4l> nt
Indexed terms for field 'title'
cocorobo (DF=1, Total TF=1)
code (DF=1, Total TF=1)
coin (DF=1, Total TF=1)
collection (DF=3, Total TF=3)
...

// pt to go back to the previous page.
nlp4l> pt
Indexed terms for field 'title'
chat (DF=1, Total TF=1)
check (DF=2, Total TF=2)
chochokure (DF=1, Total TF=1)
christian (DF=1, Total TF=1)
...
```

By default, the number of words displayed per page is 20. You can also specify a page size in the second argument like browseTerms("title",100).

In addition, passing a field name and a word to the browseTermDocs() function enables you to use the nextDocs and the prevDocs functions to browse information of documents that have the specified word in the specified field.

You can pass the number of pages you want to skip to nextDocs() and prevDocs(). Also, nextDocs(1) and prevDocs(1) define shortcut functions nd and pd respectively.

Let's browse documents that have a word "iphone" in the title field. The documents are sorted in the order of document ID that Lucene internally uses. id is a document ID whereas freq is a frequency of word (in this case, "iphone") found in the documents.

If you save the position and the offset (information that specifies where the word is located in documents. Will be discussed later) when you index documents, these information will be displayed as well.

```shell
nlp4l> browseTermDocs("title", "iphone")
Browse docs for term 'iphone' in field 'title', page size 20
Type "nextDocs(skip)" or "nd" to browse next terms.
Type "prevDocs(skip)" or "pd" to browse prev terms.

// nd to go to the next page.
nlp4l> nd
Documents for term 'iphone' in field 'title'
Doc(id=49, freq=1, positions=List(pos=5))
Doc(id=270, freq=1, positions=List(pos=0))
Doc(id=648, freq=1, positions=List(pos=0))
Doc(id=653, freq=1, positions=List(pos=2))
Doc(id=778, freq=1, positions=List(pos=2))
Doc(id=780, freq=2, positions=List(pos=0, pos=15))
...

// pd to go back to the previous page.
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

By default, the number of documents displayed per page is 20. You can also specify a page size in the third argument like browseTermDocs("title","iphone",100)

## Browsing Documents {#indexBrowser_docs}

Pass a document ID to the showDoc() function to display field values if you need to read the documents in detail.

Let's display the contents of document that corresponds to the document ID (id=1195 in this case) obtained by the browseTermDocs / nd / pd function in the last chapter. You can see that a word "iPhone" appear in the title field.

```shell
nlp4l> showDoc(1195)
Doc #1195
(Field) cat: [it-life-hack]
(Field) url: [http://news.livedoor.com/article/detail/6608703/]
(Field) title: [GoogleドライブのファイルをiPhoneからダイレクトに編集する【知っ得！虎の巻】]
...
```


## Position / Offsets{#indexBrowser_posoff}

Position / Offsets is an additional information you can save in the index that indicates the location of occurrence for each word in a document.

For example, let's look at the following document that has the sample document ID=199 so that you will know:

* There are 2 occurrence of "北海道" in the body field.
* The first occurrence is at the 126th word (position=126) and the specific character position is 247-250.
* The second occurrence is at the 129th word (position=129) and the specific character position is 255-258.

```shell
nlp4l> browseTermDocs("body","北海道")
Browse docs for term body in field 北海道, page size 20
Type "nextDocs(skip)" or "nd" to browse next terms.
Type "prevDocs(skip)" or "pd" to browse prev terms.

nlp4l> nd
Documents for term '北海道' in field 'body'
Doc(id=148, freq=1, positions=List((pos=491,offset={997-1000})))
Doc(id=199, freq=2, positions=List((pos=126,offset={247-250}), (pos=129,offset={255-258})))
...
```

## Extracting the top N words with higher frequency {#indexBrowser_topn}

Once you specify a field with the browseTerms(), you can then use the topTerms() function to obtain the top N words with higher frequency (DF) and its number of occurrences in the field.

```shell
nlp4l> browseTerms("title")
Browse terms for field title, page size 20
Type "nextTerms(skip)" or "nt" to browse next terms.
Type "prevTerms(skip)" or "pt" to browse prev terms.
Type "topTerms(n)" to find top n frequent terms.

nlp4l> topTerms(10)
Top 10 frequent terms for field title
 1: 話題 (DF=587, Total TF=607)
 2: sports (DF=493, Total TF=493)
 3: watch (DF=493, Total TF=493)
 4: 映画 (DF=373, Total TF=402)
 5: 女 (DF=319, Total TF=356)
 6: 1 (DF=318, Total TF=342)
 7: android (DF=307, Total TF=322)
 8: 女子 (DF=301, Total TF=318)
 9: 3 (DF=299, Total TF=323)
 10: アプリ (DF=291, Total TF=337)
```

# To Solr Users{#dearSolrUsers}
# To Elasticsearch Users{#dearESUsers}
# Working with Mahout{#useWithMahout}
# Working with Spark{#useWithSpark}

Through this section, Spark 1.3.0 or later must be installed beforehand. (Adjust to your version to read through if you have the version 1.3.0 or earlier installed.)

## Linking with MLLib {#useWithSpark_mllib}

With NLP4L, you can extract the feature value of corpus and give it to Spark MLlib as its input.

### Document classification with the support vector machine {#useWithSpark_svm}

Although spark MLlib provides several classifiers, we will introduce you how to perform document classification on livedoor news corpus (ldcc) using the support vector machine (SVM).

Run examples/index_ldcc.scala, if you have not run it yet, in order to prepare the ldcc corpus in the Lucene index. Then, use examples/extract_ldcc.scala to extract only the articles that have 2 categories - "dokujo-tsushin" and "sports-watch" from Lucene index - and create another Lucene index called "/tmp/index-ldcc-part".

```shell
nlp4l> :load examples/extract_ldcc.scala
```

Then, run a command line program LabeledPointAdapter as follows and output vector data of all documents in the 2 classes from /tmp/index-ldcc-part.

```shell
$ java -Dfile.encoding=UTF-8 -cp "target/pack/lib/*" org.nlp4l.spark.mllib.LabeledPointAdapter -s examples/schema/ldcc.conf -f body -l cat /tmp/index-ldcc-part
```

Here, you specify the schema definition file name as the -s option, feature vector extract target field name as the -f option, and the field name that has labels written as the -l option. The result of execution will be output to the labeled-point-out/ directory. The labeled-point-out/data.txt file is a libsvm format file that can be an input to Spark MLlib. The first column is a numeric values label while the second and the following columns are feature vectors. The correspondence relation between the numeric value labels and the actual label name is output to the labeled-point-out/label.txt file.

```shell
$ cat labeled-point-out/label.txt
dokujo-tsushin	0
sports-watch	1
```

Let's run Spark MLlib SVM now. Start spark-shell and run the following program.

```scala
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.util.MLUtils

// Load training data in LIBSVM format.
val data = MLUtils.loadLibSVMFile(sc, "labeled-point-out/data.txt")

// Split data into training (70%) and test (30%).
val splits = data.randomSplit(Array(0.7, 0.3), seed = 11L)
val training = splits(0).cache()
val test = splits(1)

// Run training algorithm to build the model
val numIterations = 100
val model = SVMWithSGD.train(training, numIterations)

// Clear the default threshold.
model.clearThreshold()

// Compute raw scores on the test set.
val scoreAndLabels = test.map { point =>
  val score = model.predict(point.features)
  (score, point.label)
}

// Get evaluation metrics.
val metrics = new BinaryClassificationMetrics(scoreAndLabels)
val auROC = metrics.areaUnderROC()

println("Area under ROC = " + auROC)
```

The result of execution will be display as follows. Area under ROC is displayed as 0.9989017178259646.

```scala
Area under ROC = 0.9989017178259646
```

### Clustering 

Several clustering algorithms (k-means, Gaussian mixture and etc.) are implemented in Spark MLlib. Among them, we will show you an example that uses LDA (Latent Dirichlet allocation). Refer to [Official Reference (v1.3.0)](http://spark.apache.org/docs/1.3.0/mllib-clustering.html) for the details of clustering algorithms that are implemented in Spark MLlib.

Side Note: LDA has been imported from Spark 1.3.0.

Run the program VectorsAdapter from the command line (Run examples/index_ldcc.scala to have the livedoor news corpus indexed beforehand).

```
$ java -Dfile.encoding=UTF-8 -cp "target/pack/lib/*" org.nlp4l.spark.mllib.VectorsAdapter -s examples/schema/ldcc.conf -f body --idfmode n --type int /tmp/index-ldcc
```

When you run the program, the following 2 files are created in the vectors-out directory.

* data.txt // A data file (comma-delimited CSV format). The first line is the header( word ID) and the first columns of each line is the document IDs.
* words.txt // A list of word ID and word pairs.

Run LDA with Spark when you have extracted the feature value. Start spark-shell and input as follows.

```shell
$ spark-shell

scala> import org.apache.spark.mllib.clustering.LDA
scala> import org.apache.spark.mllib.linalg.Vectors
// Provide data.txt as the input.
scala> val data = sc.textFile("/path/to/vectors-out/data.txt")
scala> val parsedData = data.map(s => Vectors.dense(s.trim.split(' ').map(_.toDouble)))
scala> val corpus = parsedData.zipWithIndex.map(_.swap).cache()
// Specify K=5 to create a model.
scala> val ldaModel = new LDA().setK(5).run(corpus)

// Obtain an estimated topic. A topic is expressed as occurrence probability distribution of each word.
scala> val topics = ldaModel.topicsMatrix
6.582992067532604g0.12712473287343715 2.0749605231892994 ... (5 total)
1.7458039064383513 0.00886714658883468 4.228671695274331  ...
0.993056435220057  4.64132780991838  0.18921245384121668 ...
...

// Obtain the probability distribution of each document belonging to a certain topic.
scala> val topics = ldaModel.topicDistributions
scala> topics.take(5).foreach(println)
(384,[0.13084037186524378,0.02145904901484863,0.30073967633170434,0.18175275728283377,0.36520814550536945])
(204,[0.5601036760461913,0.04276689792374281,0.17626863743620377,0.06992184061352519,0.15093894798033694])
(140,[0.01548241660044312,0.8975654153324738,0.013671563672420709,0.061526631681883964,0.011753972712778454])
(466,[0.052798328682649866,0.04602366817727088,0.7138181945792464,0.03541828992076265,0.1519415186400702])
(160,[0.20118704750574637,0.12811775189441738,0.23204896620959134,0.1428791353110324,0.29576709907921245])
```

Refer to the Spark API documents for the detailed information on how to use Spark MLlib.

# Using Lucene{#useLucene}
# Using NLP4L from Apache Zeppelin{#withZeppelin}

We will discuss how to use NLP4L from Apache Zeppelin.

## Installing Apache Zeppelin{#withZeppelin_install}

In accordance with the following procedure, install Apache Zeppelin. You can install it anywhere you want, we install Zeppelin in ~/work-zeppelin directory.

```shell
$ mkdir ~/work-zeppelin
$ cd ~/work-zeppelin
$ git clone https://github.com/apache/incubator-zeppelin.git
$ cd incubator-zeppelin
$ mvn install -DskipTests
$ cd conf
$ cp zeppelin-site.xml.template zeppelin-site.xml
```

Using an editor, open the file zeppelin-site.xml which has been copied from zeppelin-site.xml.template, add org.nlp4l.zeppelin.NLP4LInterpreter at the end of the value of the property zeppelin.interpreters as follows.

```xml
<property>
  <name>zeppelin.interpreters</name>
  <value>org.apache.zeppelin.spark.SparkInterpreter,org.apache.zeppelin.spark.PySparkInterpreter,org.apache.zeppelin.spark.SparkSqlInterpreter,org.apache.zeppelin.spark.DepInterpreter,org.apache.zeppelin.markdown.Markdown,org.apache.zeppelin.angular.AngularInterpreter,org.apache.zeppelin.shell.ShellInterpreter,org.apache.zeppelin.hive.HiveInterpreter,org.apache.zeppelin.tajo.TajoInterpreter,org.apache.zeppelin.flink.FlinkInterpreter,org.apache.zeppelin.lens.LensInterpreter,org.apache.zeppelin.ignite.IgniteInterpreter,org.apache.zeppelin.ignite.IgniteSqlInterpreter,org.nlp4l.zeppelin.NLP4LInterpreter</value>
  <description>Comma separated interpreter configurations. First interpreter become a default</description>
</property>
```

In addition, add and set false to the property zeppelin.notebook.autoInterpreterBinding in the same file.

```xml
<property>
  <name>zeppelin.notebook.autoInterpreterBinding</name>
  <value>false</value>
  <description></description>
</property>
```

## Deploying libraries of NLP4L to Apache Zeppelin{#withZeppelin_deploy}

Copy all JAR files but except zeppelin-interpreter-XXX.jar in the directory $NLP4L_HOME/target/pack/lib/ to the directory ~/work-zeppelin/incubator-zeppelin/interpreter/nlp4l/ .

```shell
$ mkdir ~/work-zeppelin/incubator-zeppelin/interpreter/nlp4l
$ cd $NLP4L_HOME
$ cp target/pack/lib/*.jar ~/work-zeppelin/incubator-zeppelin/interpreter/nlp4l
$ rm ~/work-zeppelin/incubator-zeppelin/interpreter/nlp4l/zeppelin-interpreter-*.jar
```

## Starting Apache Zeppelin{#withZeppelin_start}

Start Apache Zeppelin as follows.

```shell
$ cd ~/work-zeppelin/incubator-zeppelin
$ bin/zeppelin-daemon.sh start
```

It can be stopped as follows.

```shell
$ bin/zeppelin-daemon.sh stop
```

However, let's go to the next step without stopping it now.

## Creating a note and saving NLP4LInterpreter{#withZeppelin_save}

You can access to [http://localhost:8080/](http://localhost:8080/) from web browser. Click "Create new note" in Notebook menu to create a new note. You'll see the following on your screen, click Save button to save NLP4LInterpreter.

![Initial screen of Zeppelin Note](zeppelin-note-nlp4l-save.png)

## Executing commands or programs of NLP4L{#withZeppelin_exec}

After saving the environment, you can execute commands and programs of NLP4L in a prompt of the Apache Zeppelin Notebook. To use NLP4LInterpreter, use %nlp4l directive. Click a play button (triangle button) to execute statements you entered.

```shell
%nlp4l
open("/tmp/index-ldcc")
status
Index /tmp/index-ldcc was opened.
res0: org.nlp4l.core.RawReader = IndexReader(path='/tmp/index-ldcc',closed=false)

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

## Visualising word counts{#withZeppelin_visualize}

Using Zeppelin, let's visualize the result of word counts which we've got in [Counting the Number of Words](#useNLP_wordcounts). It's really simple because what you should do is to apply table() function.

The following program will show you any 10 words which start with letter "g" and their counts in the articles of reuters corpus.

```scala
%nlp4l

import org.nlp4l.core._
import org.nlp4l.core.analysis._
import org.nlp4l.stats.WordCounts

val index = "/tmp/index-reuters"

val reader = RawReader(index)

val allDS = reader.universalset()
val analyzer = Analyzer(new org.apache.lucene.analysis.standard.StandardAnalyzer(null.asInstanceOf[org.apache.lucene.analysis.util.CharArraySet]))
val allMap = WordCounts.count(reader, "body", Set.empty, allDS, -1, analyzer)
table(allMap.filter(_._1.startsWith("g")).take(10), "word", "count")
```

The bar chart looks like below (To see the bar chart, click the bar chart icon).

![words and their counts start with letter "g"](zeppelin-wordcounts.png)

You can use either topTermsByDocFreq() or topTermsByTotalTermFreq() of RawReader class to visualize top terms in the specific field. Note that toArray should be added at the end of these functions as the type of the first argument of the function table() Array.

```scala
%nlp4l
table(reader.topTermsByTotalTermFreq("body",5).toArray,"word","docFreq","termFreq")
```

The result you will get looks like the following (The appearance can be modified via SETTINGS menu.

![top terms](zeppelin-topterms.png)

## Visualizing Zipf's Law{#withZeppelin_zipfslaw}

Let's visualize Zipf's Law using topTermsByTotalTermFreq() . Zipf's Law says that:

> If we count up how often each word (type) of a language occurs in a large corpus, and then list the words in order of their frequency of occurrence, we can explore the relationship between the frequency of a word f and its position in the list, known as its rank r. There is a constant k such that f * r = k

The code snippet and the its chart are as follows.

```scala
%nlp4l
val index = "/tmp/index-reuters"
val reader = RawReader(index)

val sum_words = reader.sumTotalTermFreq("body")
val tttf = reader.topTermsByTotalTermFreq("body")
val top = tttf(0)._3.toFloat
table(tttf.map(a => (a._1,top/a._3.toFloat)).toArray,"word","N")
```

![Zipf's Law on Reuters Collection](zeppelin_zipfslaw.png)

# Developing and Executing NLP4L Programs{#develop}
# Attribution{#tm}
