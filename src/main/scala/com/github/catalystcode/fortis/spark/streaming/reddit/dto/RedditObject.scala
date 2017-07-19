package com.github.catalystcode.fortis.spark.streaming.reddit.dto

/**
  * A RedditObject represents the base class for all objects returned by Reddit's API endpoints. Please note that the
  * main payload of these objects resides in {@link #data}.
  *
  * @param kind String with Reddit kind code as illustrated by RedditObjectType.
  * @param data RedditObjectData with main payload of the receiver.
  */
case class RedditObject(kind: String, data: RedditObjectData)

/**
  * The RedditObjectData class represents the union of fields for objects returned by Reddit's API endpoints. Some
  * values may only apply to container RedditObject instances with certain values in their {@link RedditObject#kind}
  * field.
  *
  * @param after
  * @param before
  * @param children
  * @param created
  * @param created_utc
  * @param description
  * @param display_name
  * @param display_name_prefixed
  * @param header_img
  * @param icon_img
  * @param icon_size
  * @param id
  * @param lang
  * @param public_description
  * @param submit_text
  * @param subreddit
  * @param title
  * @param url
  */
case class RedditObjectData(author: Option[String],
                            after: Option[String],
                            before: Option[String],
                            children: List[RedditObject],
                            created: Option[Double],
                            created_utc: Option[Double],
                            description: Option[String],
                            display_name: Option[String],
                            display_name_prefixed: Option[String],
                            header_img: Option[String],
                            icon_img: Option[String],
                            icon_size: List[Int],
                            id: Option[String],
                            lang: Option[String],
                            public_description: Option[String],
                            submit_text: Option[String],
                            subreddit: Option[String],
                            title: Option[String],
                            url: Option[String])

/**
  * RedditObjectType is a convenience reference for the different values of {@link RedditObject#kind}.
  */
object RedditObjectType extends Enumeration {
  type Type = Value
  val Comment = Value("t1")
  val Account = Value("t2")
  val Link = Value("t3")
  val Message = Value("t4")
  val Subreddit = Value("t5")
  val Award = Value("t6")
  val PromoCampaign = Value("t8")
  val More = Value("more")
  val Listing = Value("listing")
}
