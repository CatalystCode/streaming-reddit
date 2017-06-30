[![Travis CI status](https://api.travis-ci.org/CatalystCode/streaming-reddit.svg?branch=master)](https://travis-ci.org/CatalystCode/streaming-reddit)

# streaming-reddit

A library for reading public search results from [Reddit](https://reddit.com/dev/api) using Spark Streaming.

## Prerequesite

Before you start using the Reddit stream for Spark, you will need to make sure you have registered a Reddit app in your account. If you have not done so, you can follow these steps:

1. Log in and click on the `preferences` link on the top right.

2. Click the `apps` tab.

3. Click the `Create an app` button and fill in the form.

## Usage example ##

Run a demo via:

```sh
# set up all the requisite environment variables
export REDDIT_APPLICATION_ID="..."
export REDDIT_APPLICATION_TOKEN="..."

# compile scala, run tests, build fat jar
sbt assembly

# run locally
java -cp target/scala-2.11/streaming-reddit-assembly-0.0.1.jar RedditDemo standalone

# run on spark
spark-submit --class RedditDemo --master local[2] target/scala-2.11/streaming-reddit-assembly-0.0.1.jar spark
```

## How does it work? ##

Currently, this streaming library polls Reddit's /r/all/search.json endpoint at interval that conforms to Reddit's API guidelines (http://github.com/reddit/reddit/wiki/API). However, at some point in the near future, this will be migrated to use Reddit live using its websockets support.

## Release process ##

1. Configure your credentials via the `SONATYPE_USER` and `SONATYPE_PASSWORD` environment variables.
2. Update `version.sbt`
3. Run `sbt sonatypeOpen "enter staging description here"`
4. Run `sbt publishSigned`
5. Run `sbt sonatypeRelease`
