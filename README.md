# What's NLP4L ?

NLP4L is a natural language processing tool for [Apache Lucene](https://lucene.apache.org/core/) written in Scala. As NLP4L processes document data registered in the Lucene index, you can directly access a word database normalized by powerful Lucene Analyzer and use handy search functions. Being written in Scala, NLP4L excels at trying ad hoc interactive processing as well. 

Refer to the following documents for more details.

- Tutorial [Japanese](http://nlp4l.github.io/tutorial_ja.html)/[English](http://nlp4l.github.io/tutorial.html) (to be available later)
- [API Docs](http://nlp4l.github.io/api/index.html)

# Target Users 

- [Apache Lucene](https://lucene.apache.org/core/), including Lucene/Solr/Elasticsearch, users.
- Users who want to try out various document vectors to input data to existing machine learning tools including [Apache Spark](https://spark.apache.org/) and [Apache Mahout](http://mahout.apache.org/).
- Other NLP tool users and Scala programers.

# Basic Usage

## Building NLP4L 

```shell
$ sbt pack
```

## Starting Interactive Shell 

```shell
$ target/pack/bin/nlp4l
Welcome to NLP4L!
Type in expressions to have them evaluated.
Type :help for more information
Type :? for information about NLP4L utilities

nlp4l> 
```

## Testing NLP4L

```shell
$ sbt test
```