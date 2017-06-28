package com.github.catalystcode.fortis.spark.streaming.reddit

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.github.catalystcode.fortis.spark.streaming.reddit.client.RedditClient
import com.github.catalystcode.fortis.spark.streaming.reddit.dto.RedditObject
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.receiver.Receiver

/**
  * The RedditInputDStream provides a Reciever that is compliant to the Reddit API usage guidelines.
  *
  * @see http://github.com/reddit/reddit/wiki/API
  */
class RedditInputDStream(val client: RedditClient,
                         val keywords: Seq[String],
                         ssc: StreamingContext,
                         storageLevel: StorageLevel,
                         val subredit: Option[String] = None,
                         val searchLimit: Int = 25,
                         val searchResultType: Option[String] = Option("link"),
                         val pollingPeriodInSeconds: Int = 2)
  extends ReceiverInputDStream[RedditObject](ssc) {

  override def getReceiver(): Receiver[RedditObject] = {
    logDebug("Creating Reddit receiver")
    new RedditReceiver(client, keywords, storageLevel, subredit, searchLimit, searchResultType, pollingPeriodInSeconds)
  }
}

/**
  * The RedditReceiver polls the Reddit API according to Reddit's guidelines, which includes a rate limit of 1 request
  * per second.
  *
  * @see http://github.com/reddit/reddit/wiki/API
  */
private class RedditReceiver(val client: RedditClient,
                             val keywords: Seq[String],
                             storageLevel: StorageLevel,
                             val subredit: Option[String] = None,
                             val searchLimit: Int = 25,
                             val searchResultType: Option[String] = Option("link"),
                             val pollingPeriodInSeconds: Int = 3)
  extends Receiver[RedditObject](storageLevel) with Logger {

  @volatile private var lastIngestedDate = Long.MinValue

  private var executor: ScheduledThreadPoolExecutor = _

  def onStart(): Unit = {
    // TODO: Consider implementing using live threads in order to eliminate the need for polling via search.
    //       https://www.reddit.com/dev/api#GET_live_{thread}

    // Please note that in order to prevent a request rate that exceeds Reddit's guidelines, the number of threads
    // is fixed to two. One of these will be dedicated to keeping the access token fresh and the other to perform the
    // actual polling.
    val threadCount = 2
    executor = new ScheduledThreadPoolExecutor(threadCount)

    client.ensureFreshToken()
    var tokenRefreshPeriodInSeconds = client.tokenExpirationInSeconds()

    // Make sure the polling period does not exceed 1 request per second.
    val normalizedPollingPeriod = Math.max(1, pollingPeriodInSeconds)

    // Make sure the refresh period isn't shorter than the polling period.
    val defaultTokenRefreshPeriod = 3600
    val normalizedRefreshPeriod = Math.max(normalizedPollingPeriod, tokenRefreshPeriodInSeconds.getOrElse(defaultTokenRefreshPeriod))

    executor.scheduleAtFixedRate(new Thread("Token refresh thread") {
      override def run(): Unit = {
        client.ensureFreshToken()
      }
    }, (normalizedRefreshPeriod-1), normalizedRefreshPeriod, TimeUnit.SECONDS)

    executor.scheduleAtFixedRate(new Thread("Polling thread") {
      override def run(): Unit = {
        poll()
      }
    }, 1, normalizedPollingPeriod, TimeUnit.SECONDS)

  }

  def onStop(): Unit = {
    if (executor != null) {
      executor.shutdown()
    }
    client.revokeToken()
  }

  protected def poll(): Unit = {
    client
      .search(keywords = keywords,
        subredit = subredit,
        limit = searchLimit,
        resultType = searchResultType
      )
      .filter(x => {
        logDebug(s"Received Reddit result ${x.data.id} from time ${x.data.created_utc}")
        isNew(x)
      })
      .foreach(x => {
        logInfo(s"Storing Reddit result ${x.data.id} from time ${x.data.created_utc}")
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

