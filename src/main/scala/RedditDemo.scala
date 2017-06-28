import com.github.catalystcode.fortis.spark.streaming.reddit.RedditAuth
import org.apache.log4j.{BasicConfigurator, Level, Logger}

object RedditDemo {
  def main(args: Array[String]) {
    val mode = args.headOption.getOrElse("")

    // configure interaction with facebook api
    val auth = RedditAuth(
      applicationId = System.getenv("REDDIT_APPLICATION_ID"),
      secret = System.getenv("REDDIT_APPLICATION_SECRET")
    )

    // configure logging
    BasicConfigurator.configure()
    Logger.getRootLogger.setLevel(Level.ERROR)
    Logger.getLogger("lib-reddit").setLevel(Level.DEBUG)

    if (mode.contains("standalone")) new RedditDemoStandalone(auth).run()
    if (mode.contains("spark")) new RedditDemoSpark(auth).run()
  }
}
