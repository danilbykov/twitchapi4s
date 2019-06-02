package io.twitchapi4s.client

import java.time.Duration

import scala.language.higherKinds
import scala.util.Try

import cats.syntax.flatMap._
import cats.syntax.functor._
import com.softwaremill.sttp._
import io.circe.HCursor
import io.circe.Decoder
import io.circe.DecodingFailure

import io.twitchapi4s.ResponseHolderPage
import io.twitchapi4s.model._

object VideosEndpoint {
  import Endpoint._

  implicit val videoDecoder = new Decoder[TwitchVideo] {
    final def apply(c: HCursor): Decoder.Result[TwitchVideo] =
      for {
        id <- c.downField("id").as[String]
        userId <- c.downField("user_id").as[String]
        userName <- c.downField("user_name").as[String]
        title <- c.downField("title").as[String]
        description <- c.downField("description").as[String]
        createdAt <- decodeInstant(c.downField("created_at").as[String], defaultFormatter)
        publishedAt <- decodeInstant(c.downField("published_at").as[String], defaultFormatter)
        url <- c.downField("url").as[String]
        thumbnailUrl <- c.downField("thumbnailUrl").as[String]
        viewable <-
          c.downField("viewable").as[String] match {
            case Right("public") => Right(Public)
            case Right("private") => Right(Private)
            case unknown => Left(DecodingFailure(s"Unknown viewable: $unknown", Nil))
          }
        viewCount <- c.downField("view_count").as[Int]
        language <- c.downField("language").as[String]
        videoType <-
          c.downField("video_type").as[String] match {
            case Right("archive") => Right(Archive)
            case Right("upload") => Right(Upload)
            case Right("highlight") => Right(Highlight)
            case unknown => Left(DecodingFailure(s"Unknown video type: $unknown", Nil))
          }
        duration <-
          c.downField("duration").as[String].flatMap { str =>
            Try(Duration.parse(str)).fold[Decoder.Result[Duration]](
              e => Left(DecodingFailure(e.getMessage, Nil)),
              Right(_)
            )
          }
      } yield TwitchVideo(id, userId, userName, title, description, createdAt, publishedAt, url,
        thumbnailUrl, viewable, viewCount, language, videoType, duration)
  }
}

trait VideosEndpoint[F[_]] extends Endpoint[F] {
  import VideosEndpoint._
  import Endpoint._

  val videosUrl = s"${root}helix/videos"

  def getVideos(
    period: VideoPeriod = AllTime,
    sort: VideoSort = TimeSort,
    tpe: Option[VideoType] = None,
    language: Option[String] = None,
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  ): F[ResponseHolderPage[List[TwitchVideo]]] =
    for {
      env <- applicaveAsk.ask
      request = sttp.get(uri"$videosUrl?first=$first&after=$after&before=$before&language=$language&period=${period.repr}&sort=${sort.repr}&type=${tpe.map(_.repr).getOrElse("all")}")
      maybeHttpResponse <- monadError.attempt(request.twitchAuth(env).send())
      result <- parseHttpResponse[ResponseHolderPage[List[TwitchVideo]]](maybeHttpResponse)
    } yield result
}
