package com.github.catalystcode.fortis.spark.streaming.reddit

import com.github.catalystcode.fortis.spark.streaming.reddit.client.RedditClient
import com.github.catalystcode.fortis.spark.streaming.reddit.dto.RedditObject
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.ReceiverInputDStream

object RedditUtils {
  def createPageStream(redditAuth: RedditAuth,
                       keywords: Seq[String],
                       ssc: StreamingContext,
                       storageLevel: StorageLevel = StorageLevel.MEMORY_ONLY,
                       pollingPeriodInSeconds: Int = 3,
                       tokenRefreshPeriodInSeconds: Int = (3600/2)
  ): ReceiverInputDStream[RedditObject] = {
    return new RedditInputDStream(
      client = new RedditClient(redditAuth.applicationId, redditAuth.secret),
      keywords = keywords,
      ssc = ssc,
      storageLevel = storageLevel,
      pollingPeriodInSeconds = pollingPeriodInSeconds,
      tokenRefreshPeriodInSeconds = tokenRefreshPeriodInSeconds)
  }
}
