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
                       subredit: Option[String] = None,
                       searchLimit: Int = 25,
                       searchResultType: Option[String] = Option("link")
  ): ReceiverInputDStream[RedditObject] = {
    return new RedditInputDStream(
      client = new RedditClient(redditAuth.applicationId, redditAuth.secret),
      keywords = keywords,
      ssc = ssc,
      storageLevel = storageLevel,
      subredit = subredit,
      searchLimit = searchLimit,
      searchResultType = searchResultType,
      pollingPeriodInSeconds = pollingPeriodInSeconds)
  }
}
