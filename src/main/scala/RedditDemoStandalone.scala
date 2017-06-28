import java.util.Date

import com.github.catalystcode.fortis.spark.streaming.reddit.RedditAuth
import com.github.catalystcode.fortis.spark.streaming.reddit.client.RedditClient

class RedditDemoStandalone(auth: RedditAuth) {
  def run(): Unit = {
    val keywords = List("healthcare")
    val client = new RedditClient(auth.applicationId, auth.secret)
    client.ensureFreshToken()
    println(client.search(keywords, limit = 2).toList)
    client.ensureFreshToken()
    client.revokeToken()
  }
}
