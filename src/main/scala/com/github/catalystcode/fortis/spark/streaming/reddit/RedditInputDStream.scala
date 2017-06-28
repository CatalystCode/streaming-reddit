package com.github.catalystcode.fortis.spark.streaming.reddit

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.github.catalystcode.fortis.spark.streaming.reddit.client.RedditClient
import com.github.catalystcode.fortis.spark.streaming.reddit.dto.RedditObject
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.receiver.Receiver

/**
  * A RedditInputDStream is a concrete implementation of a {@link ReceiverInputDStream} that provides a {@link Receiver}
  * instance that is compliant to the Reddit API usage guidelines.
  *
  * @see http://github.com/reddit/reddit/wiki/API
  */
class RedditInputDStream(val client: RedditClient,
                         val keywords: Seq[String],
                         ssc: StreamingContext,
                         storageLevel: StorageLevel,
                         val pollingPeriodInSeconds: Int = 2,
                         val tokenRefreshPeriodInSeconds: Int = (3600/2))
  extends ReceiverInputDStream[RedditObject](ssc) {

  override def getReceiver(): Receiver[RedditObject] = {
    logDebug("Creating Reddit receiver")
    new RedditReceiver(client, keywords, storageLevel, pollingPeriodInSeconds)
  }
}

/**
  * The RedditReceiver is a concrete implementation of a {@link Receiver} that polls the Reddit API according to
  * Reddit's guidelines, which includes a rate limit of 1 request per 2 seconds.
  *
  * @see http://github.com/reddit/reddit/wiki/API
  */
private class RedditReceiver(val client: RedditClient,
                             val keywords: Seq[String],
                             storageLevel: StorageLevel,
                             val pollingPeriodInSeconds: Int = 3)
  extends Receiver[RedditObject](storageLevel) with Logger {

  @volatile private var lastIngestedDate = Long.MinValue

  private final val executor: ScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2)

  def onStart(): Unit = {
    // TODO: Consider implementing using live threads in order to eliminate the need for polling via search.
    //       https://www.reddit.com/dev/api#GET_live_{thread}

    client.ensureFreshToken()
    var tokenRefreshPeriodInSeconds = client.tokenExpirationInSeconds()

    // Make sure the polling period does not exceed 1 request per every 2 seconds.
    val normalizedPollingPeriod = Math.max(2, tokenRefreshPeriodInSeconds.get)

    // Make sure the refresh period isn't shorter than the polling period.
    val normalizedRefreshPeriod = Math.max(normalizedPollingPeriod, tokenRefreshPeriodInSeconds.get)

    executor.scheduleAtFixedRate(new Thread("Token refresh thread") {
      override def run(): Unit = {
        client.ensureFreshToken()
      }
    }, 0, normalizedRefreshPeriod, TimeUnit.SECONDS)

    executor.scheduleAtFixedRate(new Thread("Polling thread") {
      override def run(): Unit = {
        poll()
      }
    }, 1, normalizedPollingPeriod, TimeUnit.SECONDS)

  }

  def onStop(): Unit = {
    if (executor != null) {
      executor.shutdown()

      client.revokeToken()
    }
  }

  protected def poll(): Unit = {
    client
      .search(keywords = keywords)
      .filter(x => {
        logDebug(s"Received Reddit result ${x.data.public_description} from time ${x.data.created_utc}")
        isNew(x)
      })
      .foreach(x => {
        logInfo(s"Storing Reddit result ${x.data.url}")
        store(x)
        markStored(x)
      })
  }

  private def isNew(item: RedditObject): Boolean = {
    if (item.data.created_utc.isEmpty) {
      return false
    }
    val createdAt = item.data.created_utc.get.toLong
    createdAt > lastIngestedDate
  }

  private def markStored(item: RedditObject): Unit = {
    if (isNew(item)) {
      val createdAt = item.data.created_utc.get.toLong
      lastIngestedDate = createdAt
      logDebug(s"Updating last ingested date to ${createdAt}")
    }
  }
}

