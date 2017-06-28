import com.github.catalystcode.fortis.spark.streaming.reddit.{RedditAuth, RedditUtils}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

class RedditDemoSpark(auth: RedditAuth) {
  def run(): Unit = {
    // set up the spark context and streams
    val conf = new SparkConf().setAppName("Reddit Application").setIfMissing("spark.master", "local[*]")
    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, Seconds(1))
    val keywordSet = List("healthcare")

    RedditUtils.createPageStream(auth, keywordSet, ssc).map(x => s"Post: ${x}").print()

    // run forever
    ssc.start()
    ssc.awaitTermination()
  }

}
