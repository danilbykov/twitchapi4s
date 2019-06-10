package io.twitchapi4s.client

import java.time.Duration
import java.time.Instant

import scala.language.higherKinds
import scala.util.Try

import cats.mtl.ApplicativeAsk
import cats.mtl.MonadState
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.softwaremill.sttp._
import io.circe.HCursor
import io.circe.Decoder
import io.circe.DecodingFailure

import io.twitchapi4s.RecoverableTwitchEnv
import io.twitchapi4s.ResponseHolderPage
import io.twitchapi4s.TwitchEnv
import io.twitchapi4s.model._

object VideosEndpoint {
  import TwitchEndpoint._

  implicit val videoDecoder = new Decoder[TwitchVideo] {
    final def apply(c: HCursor): Decoder.Result[TwitchVideo] =
      for {
        id <- c.downField("id").as[String]
        userId <- c.downField("user_id").as[String]
        userName <- c.downField("user_name").as[String]
        title <- c.downField("title").as[String]
        description <- c.downField("description").as[String]
        createdAt <- decodeInstant(c.downField("created_at").as[String], defaultFormatter)
        publishedAt <- c.downField("published_at").as[Option[String]].map(_.flatMap(str =>
          Try(Instant.from(defaultFormatter.parse(str))).toOption
        ))
        url <- c.downField("url").as[String]
        thumbnailUrl <- c.downField("thumbnail_url").as[String]
        viewable <-
          c.downField("viewable").as[String] match {
            case Right("public") => Right(Public)
            case Right("private") => Right(Private)
            case unknown => Left(DecodingFailure(s"Unknown viewable: $unknown", Nil))
          }
        viewCount <- c.downField("view_count").as[Int]
        language <- c.downField("language").as[String]
        videoType <-
          c.downField("type").as[String] match {
            case Right("archive") => Right(Archive)
            case Right("upload") => Right(Upload)
            case Right("highlight") => Right(Highlight)
            case unknown => Left(DecodingFailure(s"Unknown video type: $unknown", Nil))
          }
        duration <-
          c.downField("duration").as[String].flatMap { str =>
            Try(Duration.parse(s"PT$str")).fold[Decoder.Result[Duration]](
              e => Left(DecodingFailure(e.getMessage, Nil)),
              Right(_)
            )
          }
      } yield TwitchVideo(id, userId, userName, title, description, createdAt, publishedAt, url,
        thumbnailUrl, viewable, viewCount, language, videoType, duration)
  }
}

trait VideosEndpoint[F[_]] extends TwitchEndpoint[F] {
  import VideosEndpoint._
  import TwitchEndpoint._

  val videosUrl = s"${root}helix/videos"

  def getVideos(
    env: TwitchEnv
  )(
    videoId: VideoId,
    period: VideoPeriod = AllTime,
    sort: VideoSort = TimeSort,
    tpe: Option[VideoType] = None,
    language: Option[String] = None,
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  ): F[ResponseHolderPage[List[TwitchVideo]]] = {
    val (ids, userId, gameId) = videoId match {
      case GameVideoId(gameId) => (Nil, "", gameId)
      case UserVideoId(userId) => (Nil, userId, "")
      case VideoIds(ids) => (ids, "", "")
    }
    for {
      request <- sttp.get(uri"$videosUrl?id=$ids&user_id=$userId&game_id=$gameId&first=$first&after=$after&before=$before&language=$language&period=${period.repr}&sort=${sort.repr}&type=${tpe.map(_.repr).getOrElse("all")}").pure
      maybeHttpResponse <- request.twitchAuth(env).send().attempt
      result <- parseHttpResponse[ResponseHolderPage[List[TwitchVideo]]](maybeHttpResponse)
    } yield result
  }

  def getVideosR(
    videoId: VideoId,
    period: VideoPeriod = AllTime,
    sort: VideoSort = TimeSort,
    tpe: Option[VideoType] = None,
    language: Option[String] = None,
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  )(implicit aa: ApplicativeAsk[F, TwitchEnv]): F[ResponseHolderPage[List[TwitchVideo]]] =
    loadWithApplicativeAsk((env) => getVideos(env)(videoId, period, sort, tpe, language, after, before, first))

  def getVideosS(
    videoId: VideoId,
    period: VideoPeriod = AllTime,
    sort: VideoSort = TimeSort,
    tpe: Option[VideoType] = None,
    language: Option[String] = None,
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  )(implicit ms: MonadState[F, RecoverableTwitchEnv]): F[ResponseHolderPage[List[TwitchVideo]]] =
    loadWithMonadState((env) => getVideos(env)(videoId, period, sort, tpe, language, after, before, first))
}
