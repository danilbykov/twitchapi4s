package io.twitchapi4s.client

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
import io.twitchapi4s.ResponseHolderPage
import io.twitchapi4s.TwitchEnv
import io.twitchapi4s.model._

object StreamsEndpoint {
  import Endpoint._

  implicit val videoDecoder = new Decoder[TwitchStream] {
    final def apply(c: HCursor): Decoder.Result[TwitchStream] =
      for {
        communityIds <- c.downField("community_ids").as[List[String]]
        gameId <- c.downField("game_id").as[String]
        id <- c.downField("id").as[String]
        language <- c.downField("language").as[String]
        startedAt <- decodeInstant(c.downField("started_at").as[String], defaultFormatter)
        tagIds <- c.downField("tag_ids").as[List[String]]
        thumbnailUrl <- c.downField("thumbnail_url").as[String]
        title <- c.downField("title").as[String]
        tpe <-
          c.downField("type").as[String] match {
            case Right("live") => Right(true)
            case Right("") => Right(false)
            case unknown => Left(DecodingFailure(s"Unknown stream type: $unknown", Nil))
          }
        userId <- c.downField("user_id").as[String]
        userName <- c.downField("user_name").as[String]
        viewerCount <- c.downField("viewer_count").as[Int]
      } yield TwitchStream(communityIds, gameId, id, language, startedAt, tagIds, thumbnailUrl,
        title, tpe, userId, userName, viewerCount)
  }
}

trait StreamsEndpoint[F[_]] extends Endpoint[F] {
  import Endpoint._
  import StreamsEndpoint._

  val streamsUrl = s"${root}helix/streams"

  def getStreams(
    env: TwitchEnv
  )(
    communityIds: List[String],
    gameIds: List[String],
    languages: List[String],
    userIds: List[String],
    userLogins: List[String],
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  ): F[ResponseHolderPage[List[TwitchStream]]] =
    for {
      request <- sttp.get(uri"$streamsUrl?communityIds=$communityIds&game_id=$gameIds&language=$languages&user_id=$userIds&user_login=$userLogins&first=$first&before=$before&after=$after").pure
      maybeHttpResponse <- request.twitchAuth(env).send().attempt
      result <- parseHttpResponse[ResponseHolderPage[List[TwitchStream]]](maybeHttpResponse)
    } yield result

  def getStreamsR(
    communityIds: List[String],
    gameIds: List[String],
    languages: List[String],
    userIds: List[String],
    userLogins: List[String],
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  )(implicit aa: ApplicativeAsk[F, TwitchEnv]): F[ResponseHolderPage[List[TwitchStream]]] =
    loadWithApplicativeAsk((env) => getStreams(env)(communityIds, gameIds, languages, userIds, userLogins,
      after, before, first))

  def getStreamsS(
    communityIds: List[String],
    gameIds: List[String],
    languages: List[String],
    userIds: List[String],
    userLogins: List[String],
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  )(implicit ms: MonadState[F, RecoverableTwitchEnv]): F[ResponseHolderPage[List[TwitchStream]]] =
    loadWithMonadState((env) => getStreams(env)(communityIds, gameIds, languages, userIds, userLogins,
      after, before, first))
}
