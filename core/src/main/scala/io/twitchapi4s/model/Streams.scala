package io.twitchapi4s.model

import java.time.Instant

/**
  * Represents an active Twitch stream
  *
  * @param communityIds Sequence of communities related to this stream
  * @param gameId Id of the game being played on the stream
  * @param id Stream id
  * @param language Language of the stream
  * @param startedAt UTC timestamp
  * @param thumbnailUrl Thumbnail URL of the stream
  * @param title Stream title
  * @param streamType Stream type: "live", "vodcast", or ""
  * @param userId ID of the user who is streaming
  * @param viewerCount Number of viewers watching the stream at the time of the query
  */
case class TwitchStream(
  communityIds: List[String],
  gameId: String,
  id: String,
  language: String,
  startedAt: Instant,
  tagIds: List[String],
  thumbnailUrl: String,
  title: String,
  isLive: Boolean,
  userId: String,
  userName: String,
  viewerCount: Long
)
