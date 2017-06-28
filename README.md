# streaming-reddit

A library for reading public search results from [Reddit](https://reddit.com/dev/api) using Spark Streaming.
[![Travis CI status](https://api.travis-ci.org/CatalystCode/streaming-reddit.svg?branch=master)](https://travis-ci.org/CatalystCode/streaming-reddit)

## Usage example ##

Run a demo via:

```sh
# set up all the requisite environment variables
export REDDIT_APPLICATION_ID="..."
export REDDIT_APPLICATION_TOKEN="..."

# compile scala, run tests, build fat jar
sbt assembly

# run locally
java -cp target/scala-2.11/streaming-reddit-assembly-0.0.7.jar RedditDemo standalone

# run on spark
spark-submit --class RedditDemo --master local[2] target/scala-2.11/streaming-reddit-assembly-0.0.7.jar spark
```

## How does it work? ##

Currently, this streaming library polls Reddit's /r/all/search.json endpoint at interval that conforms to Reddit's API guidelines (http://github.com/reddit/reddit/wiki/API). However, at some point in the near future, this will be migrated to use Reddit live using its websockets support.

## Release process ##

1. Configure your credentials via the `SONATYPE_USER` and `SONATYPE_PASSWORD` environment variables.
2. Update `version.sbt`
3. Run `sbt sonatypeOpen "enter staging description here"`
4. Run `sbt publishSigned`
5. Run `sbt sonatypeRelease`
