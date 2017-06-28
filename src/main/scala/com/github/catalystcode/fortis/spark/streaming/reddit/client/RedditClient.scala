package com.github.catalystcode.fortis.spark.streaming.reddit.client

import java.net.URLEncoder
import java.util.UUID

import com.github.catalystcode.fortis.spark.streaming.reddit.Logger
import com.github.catalystcode.fortis.spark.streaming.reddit.dto.RedditObject
import net.liftweb.json

import scalaj.http._

/**
  * The RedditClient class provides two public-facing methods:
  * <ul>
  *   <li>{@link #ensureFreshToken()}</li>
  *   <li>{@link #search()}</li>
  * </ul>
  *
  * @param applicationId
  * @param secret
  * @param fetcher
  */
@SerialVersionUID(100L)
class RedditClient(val applicationId: String,
                   val secret: String,
                   val fetcher: ResponseFetcher = new DefaultResponseFetcher())
  extends Serializable with Logger {

  protected val userAgent = s"FotisReddit/0.1 by ${this.applicationId}"
  protected val deviceId = UUID.randomUUID().toString
  protected var clientCredentials: Option[ClientCredentials] = None

  /**
    * Attempts to fetch a fresh token from Reddit. If no valid token is present, a new one is fetched. Otherwise, an
    * attempt to refresh the existing token is made.
    */
  def ensureFreshToken(): Unit = {
    if (clientCredentials.isEmpty) {
      fetchAccessToken()
    }
    else {
      try {
        refreshToken()
      }
      catch {
        case ex: Exception =>
          logError(s"Exception while attempting to refresh Reddit token", ex)
          fetchAccessToken()
      }
    }
  }

  def tokenExpirationInSeconds(): Option[Int] = {
    clientCredentials.map(c=>c.expires_in)
  }

  private def fetchAccessToken(): Unit = {
    val request = Http("https://www.reddit.com/api/v1/access_token")
      .method("POST")
      .auth(applicationId, secret)
      .header("User-Agent", userAgent)
      .postForm(Seq("grant_type"->"client_credentials", "device_id"->this.deviceId))
    val stringResponse = fetcher.fetchStringResponse(request)

    implicit val formats = json.DefaultFormats
    this.clientCredentials = Option(json.parse(stringResponse).extract[ClientCredentials])
  }

  private def refreshToken(): Unit = {
    if (this.clientCredentials.isEmpty) {
      return
    }

    val request = Http("https://www.reddit.com/api/v1/access_token")
      .method("POST")
      .auth(applicationId, secret)
      .header("User-Agent", userAgent)
      .postForm(Seq("grant_type"->"refresh_token", "refresh_token"->this.clientCredentials.get.access_token))

    implicit val formats = json.DefaultFormats
    json.parse(fetcher.fetchStringResponse(request)).extract[ClientCredentials]
  }

  /**
    * Invalidates the authentication token associated with the receiver, if one is present.
    */
  def revokeToken(): Unit = {
    if (this.clientCredentials.isEmpty) {
      return
    }

    val request = Http("https://www.reddit.com/api/v1/revoke_token")
      .method("POST")
      .auth(applicationId, secret)
      .header("User-Agent", userAgent)
      .postForm(Seq("token_type_hint"->"access_token", "token"->this.clientCredentials.get.access_token))
    fetcher.fetchStringResponse(request)
  }

  protected def authorizationHeader(): Option[String] = {
    if (this.clientCredentials.isEmpty) None
    else Option(s"${this.clientCredentials.get.token_type} ${this.clientCredentials.get.access_token}")
  }

  /**
    * @param keywords
    * @return Optional string containing a space-separated, quoted, sequence of all keywords
    */
  private def keywordsAsSearchTerm(keywords: Seq[String]): Option[String] = {
    keywords match {
      case Nil => None
      case k :: ks => Option(keywords.mkString(" "))
    }
  }

  /**
    * @return Iterable set of RedditObject instances that represent the items returned from Reddit.
    * @see https://www.reddit.com/dev/api#GET_search
    */
  def search(
              keywords: Seq[String],
              limit: Int = 25,
              subredit: Option[String] = None,
              after: Option[String] = None,
              before: Option[String] = None,
              count: Int = 0,
              include_facets: Boolean = false,
              restrict_sr: Boolean = false,
              show: Option[String] = Option("all"),
              sort: String = "new",
              sr_detail: Option[Boolean] = Option(true),
              syntax: String = "plain",
              t: String = "hour",
              resultType: Option[String] = Option("link")
            ): Iterable[RedditObject] = {

    val keywordsString = this.keywordsAsSearchTerm(keywords)
    if (keywordsString.isEmpty) {
      logDebug("No search terms present, so returning an empty list.")
      return List()
    }

    val authorizationHeader = this.authorizationHeader()
    if (authorizationHeader.isEmpty) {
      logDebug("No authorization header present, so returning an empty list.")
      return List()
    }

    try {
      val subreditPath = if (subredit.isDefined) s"r/${URLEncoder.encode(subredit.get, "UTF8")}" else "r/all"
      val baseUrl = s"https://oauth.reddit.com/${subreditPath}/search.json"
      var request = Http(baseUrl)
        .method("GET")
        .header("User-Agent", userAgent)
        .header("Authorization", authorizationHeader.get)
        .param("q", keywordsString.get)
        .param("limit", limit.toString)
        .param("count", count.toString)
        .param("include_facets", include_facets.toString)
        .param("restrict_sr", restrict_sr.toString)
        .param("sort", sort)
        .param("syntax", syntax)
        .param("t", t)
      if (before.isDefined) request = request.param("before", before.get)
      if (after.isDefined) request = request.param("after", after.get)
      if (show.isDefined) request = request.param("show", show.get)
      if (sr_detail.isDefined) request = request.param("sr_detail", sr_detail.get.toString)
      if (resultType.isDefined) request = request.param("sr_detail", resultType.get.toString)

      val responseString = fetcher.fetchStringResponse(request)

      implicit val formats = json.DefaultFormats
      val responseJson = json.parse(responseString)
      val response = responseJson.extract[RedditObject]
      return response.data.children
    } catch {
      case ex: Exception =>
        logError(s"Exception while loading reddit search results", ex)
        None
    }
  }

}

trait ResponseFetcher extends Serializable {
  def fetchStringResponse(request: HttpRequest): String
}

@SerialVersionUID(100L)
class DefaultResponseFetcher extends ResponseFetcher with Logger {
  override def fetchStringResponse(request: HttpRequest): String = {
    logInfo(s"Performing ${request.method} request from ${request.url}")
    return request.asString.body
  }
}

case class ClientCredentials(val access_token: String, val token_type: String, val expires_in: Int)
