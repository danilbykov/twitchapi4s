package io.twitchapi4s.client

import java.time.Instant

import scala.language.higherKinds

import cats.mtl.ApplicativeAsk
import cats.mtl.MonadState
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.softwaremill.sttp._
import io.circe._

import io.twitchapi4s.RecoverableTwitchEnv
import io.twitchapi4s.ResponseHolder
import io.twitchapi4s.ResponseHolderPage
import io.twitchapi4s.TwitchEnv
import io.twitchapi4s.model._

object UsersEndpoint {
  import Endpoint._

  implicit val userDecoder = new Decoder[TwitchUser] {
    final def apply(c: HCursor): Decoder.Result[TwitchUser] =
      for {
        broadcaster <-
          c.downField("broadcaster_type").as[String] match {
            case Right("partner") => Right(Some(Partner))
            case Right("affiliate") => Right(Some(Affiliate))
            case Right("") => Right(None)
            case unknown => Left(DecodingFailure(s"Unknown broadcaster type: $unknown", Nil))
          }
        description <- c.downField("description").as[String]
        displayName <- c.downField("display_name").as[String]
        email <- c.downField("email").as[Option[String]]
        id <- c.downField("id").as[String]
        login <- c.downField("login").as[String]
        offlineImageUrl <- c.downField("offline_image_url").as[String]
        profileImageUrl <- c.downField("profile_image_url").as[String]
        tpe <-
          c.downField("type").as[String] match {
            case Right("staff") => Right(Some(Staff))
            case Right("admin") => Right(Some(Admin))
            case Right("global_mod") => Right(Some(GlobalMod))
            case Right("") => Right(None)
            case unknown => Left(DecodingFailure(s"Unknown user type: $unknown", Nil))
          }
        viewCount <- c.downField("view_count").as[Int]
      } yield TwitchUser(
        broadcaster, description, displayName, email, id, login, offlineImageUrl, profileImageUrl,
        tpe, viewCount
      )
  }

  implicit val twitchFollowDecoder = new Decoder[TwitchFollow] {
    final def apply(c: HCursor): Decoder.Result[TwitchFollow] =
      for {
        fromId <- c.downField("from_id").as[String]
        fromName <- c.downField("from_name").as[String]
        toId <- c.downField("to_id").as[String]
        toName <- c.downField("to_name").as[String]
        followedAt <- decodeInstant(c.downField("followed_at").as[String], defaultFormatter)
      } yield TwitchFollow(fromId, fromName, toId, toName, Instant.from(followedAt))
  }
}

trait UsersEndpoint[F[_]] extends Endpoint[F] {
  import Endpoint._
  import UsersEndpoint._

  val usersUrl = s"${root}helix/users"

  val usersFollowsUrl = s"${root}helix/users/follows"

  def getUsers(
    env: TwitchEnv
  )(
    ids: List[String],
    logins: List[String]
  ): F[List[TwitchUser]] =
    for {
      request <- sttp.get(uri"$usersUrl?id=$ids&login=$logins").twitchAuth(env).pure
      maybeHttpResponse <- request.send().attempt
      result <- parseHttpResponse[ResponseHolder[List[TwitchUser]]](maybeHttpResponse)
    } yield result.data

  def getUsersR(
    ids: List[String],
    logins: List[String]
  )(implicit aa: ApplicativeAsk[F, TwitchEnv]): F[List[TwitchUser]] =
    loadWithApplicativeAsk((env) => getUsers(env)(ids, logins))

  def getUsersS(
    ids: List[String],
    logins: List[String]
  )(implicit ms: MonadState[F, RecoverableTwitchEnv]): F[List[TwitchUser]] =
    loadWithMonadState((env) => getUsers(env)(ids, logins))

  def getFollows(
    env: TwitchEnv
  )(
    fromId: Option[String],
    toId: Option[String],
    after: Option[String] = None,
    first: Int = 20
  ): F[ResponseHolderPage[List[TwitchFollow]]] =
    for {
      request <- sttp.get(uri"$usersFollowsUrl?from_id=$fromId&to_id=$toId&first=$first&after=$after").pure
      maybeHttpResponse <- request.twitchAuth(env).send().attempt
      result <- parseHttpResponse[ResponseHolderPage[List[TwitchFollow]]](maybeHttpResponse)
    } yield result

  def getFollowsR(
    fromId: Option[String],
    toId: Option[String],
    after: Option[String] = None,
    first: Int = 20
  )(implicit aa: ApplicativeAsk[F, TwitchEnv]): F[ResponseHolderPage[List[TwitchFollow]]] =
    loadWithApplicativeAsk((env) => getFollows(env)(fromId, toId, after, first))

  def getFollowsS(
    fromId: Option[String],
    toId: Option[String],
    after: Option[String] = None,
    first: Int = 20
  )(implicit aa: MonadState[F, RecoverableTwitchEnv]): F[ResponseHolderPage[List[TwitchFollow]]] =
    loadWithMonadState((env) => getFollows(env)(fromId, toId, after, first))
}
