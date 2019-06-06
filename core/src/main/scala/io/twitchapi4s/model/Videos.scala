package io.twitchapi4s.model

import java.time.Duration
import java.time.Instant

sealed trait VideoId
case class GameVideoId(gameId: String) extends VideoId
case class UserVideoId(userId: String) extends VideoId
case class VideoIds(ids: List[String]) extends VideoId

sealed abstract class VideoPeriod(val repr: String)
case object AllTime extends VideoPeriod("all")
case object Day extends VideoPeriod("day")
case object Week extends VideoPeriod("week")
case object Month extends VideoPeriod("month")

sealed abstract class VideoSort(val repr: String)
case object TimeSort extends VideoSort("time")
case object TrendingSort extends VideoSort("trending")
case object ViewsSort extends VideoSort("views")

sealed trait ViewableType
case object Public extends ViewableType
case object Private extends ViewableType

sealed abstract class VideoType(val repr: String)
case object Upload extends VideoType("upload")
case object Archive extends VideoType("archive")
case object Highlight extends VideoType("highlight")

/**
  * Represents a Twitch video
  *
  * @param id           ID of the video
  * @param language     Language of the video
  * @param publishedAt  Date when the video was published
  * @param thumbnailUrl Template URL for the thumbnail of the video
  * @param title        Title of the video
  * @param url          Trl of the video
  * @param userId       Id of the user who owns the video
  * @param viewCount    Number of times the video has been viewed
  * @param viewableType Indicates whether the video is publicly viewable
  * @param videoType    Type of video
  * @param createdAt    Date when the video was created
  * @param description  Description of the video
  * @param duration     Length of the video
  */
case class TwitchVideo(
  id: String,
  userId: String,
  userName: String,
  title: String,
  description: String,
  createdAt: Instant,
  publishedAt: Option[Instant],
  url: String,
  thumbnailUrl: String,
  viewable: ViewableType,
  viewCount: Int,
  language: String,
  videoType: VideoType,
  duration: Duration
)
