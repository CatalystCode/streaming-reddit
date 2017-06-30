[![Travis CI status](https://api.travis-ci.org/CatalystCode/streaming-reddit.svg?branch=master)](https://travis-ci.org/CatalystCode/streaming-reddit)

# streaming-reddit

A library for reading public search results from [Reddit](https://reddit.com/dev/api) using Spark Streaming.

## Prerequesite

Before you start using the Reddit stream for Spark, you will need to make sure you have registered a Reddit app in your account. If you have not done so, you can follow these steps:

1. Log in and click on the `preferences` link on the top right.

<img width="1026" alt="reddit_login_welcome" src="https://user-images.githubusercontent.com/1117904/27742298-c0cc0d10-5d7d-11e7-9856-fc1a21a3695b.png">

2. Click the `apps` tab.

<img width="1026" alt="reddit_preferences" src="https://user-images.githubusercontent.com/1117904/27742319-d1f9f3ea-5d7d-11e7-9714-c0c3eb9c34bc.png">

3. Click the `Create an app` button and fill in the form.

<img width="968" alt="reddit_create_an_app" src="https://user-images.githubusercontent.com/1117904/27742327-dc993f2c-5d7d-11e7-9dee-37249c31ff18.png">


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

Add to your own project by adding this dependency in your `build.sbt`:

```
libraryDependencies ++= Seq(
  //...
  "com.github.catalystcode" %% "streaming-reddit" % "0.0.1",
  //...
)
```

## How does it work? ##

Currently, this streaming library polls Reddit's /r/all/search.json endpoint at interval that conforms to Reddit's API guidelines (http://github.com/reddit/reddit/wiki/API). However, at some point in the near future, this will be migrated to use Reddit live using its websockets support.

## Release process ##

1. Configure your credentials via the `SONATYPE_USER` and `SONATYPE_PASSWORD` environment variables.
2. Update `version.sbt`
3. Run `sbt sonatypeOpen "enter staging description here"`
4. Run `sbt publishSigned`
5. Run `sbt sonatypeRelease`
