package io.twitchapi4s.model

import java.time.Instant

sealed trait BroadcasterType
case object Partner extends BroadcasterType
case object Affiliate extends BroadcasterType

sealed trait UserType
case object Staff extends UserType
case object Admin extends UserType
case object GlobalMod extends UserType

/**
  * Represents Twitch user data
  *
  * @param broadcasterType User’s broadcaster type
  * @param description User’s channel description
  * @param displayName User’s display name
  * @param email User’s email address
  * @param id User’s ID
  * @param login User’s login name
  * @param offlineImageUrl URL of the user’s offline image
  * @param profileImageUrl URL of the user’s profile image
  * @param userType User’s type
  * @param viewCount Total number of views of the user’s channel
  */
case class TwitchUser(
  broadcasterType: Option[BroadcasterType],
  description: String,
  displayName: String,
  email: Option[String],
  id: String,
  login: String,
  offlineImageUrl: String,
  profileImageUrl: String,
  userType: Option[UserType],
  viewCount: Int
)

/**
  * Represents the relationships between two Twitch users
  *
  * @param fromId ID of the user following the to_id user
  * @param fromName Display name corresponding to fromId
  * @param toId ID of the user being followed by the from_id user
  * @param toName Display name corresponding to to_id
  * @param followedAt Date and time when the from_id user followed the to_id user
  */
case class TwitchFollow(
  fromId: String,
  fromName: String,
  toId: String,
  toName: String,
  followedAt: Instant
)
