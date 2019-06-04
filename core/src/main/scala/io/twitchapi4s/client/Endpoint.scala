package io.twitchapi4s.client

import java.time.format.DateTimeFormatter
import java.time.Instant

import scala.language.higherKinds
import scala.util.Try

import cats.MonadError
import cats.mtl.ApplicativeAsk
import cats.syntax.applicative._
import com.softwaremill.sttp._
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Json
import io.circe.HCursor
import io.circe.generic.auto._
import io.circe.parser

import io.twitchapi4s._

object Endpoint {

  implicit class RequestTOps[U[_], T, +S](requestT: RequestT[U, T, S]) {
    def twitchAuth(env: TwitchEnv) =
      env match {
        case ClientTwitchEnv(clientId) =>
          requestT.header("Client-ID", clientId)
      }
  }

  implicit def twitchResponseDecoder[D: Decoder] = new Decoder[ResponseHolder[D]] {
    final def apply(c: HCursor): Decoder.Result[ResponseHolder[D]] =
      c.downField("data").as[D].map(ResponseHolder(_))
  }

  implicit def twitchResponseWithHolderDecoder[D: Decoder] = new Decoder[ResponseHolderPage[D]] {
    final def apply(c: HCursor): Decoder.Result[ResponseHolderPage[D]] =
      for {
        data <- c.downField("data").as[D]
        //total <- c.downField("total").as[Int]
        pagination <- c.downField("pagination").as[Pagination]
      } yield ResponseHolderPage(data/*, total*/, pagination)
  }

  val defaultFormatter = DateTimeFormatter.ISO_INSTANT

  def decodeInstant(rawTime: Decoder.Result[String], formatter: DateTimeFormatter) =
    rawTime.flatMap { str =>
      Try(formatter.parse(str)).fold[Decoder.Result[Instant]](
        e => Left(DecodingFailure(e.getMessage, Nil)),
        i => Right(Instant.from(i))
      )
    }
}

trait Endpoint[F[_]] {

  implicit val monadError: MonadError[F, TwitchApiException]
  implicit val applicativeAsk: ApplicativeAsk[F, TwitchEnv]

  implicit val sttpBackend: SttpBackend[F, Nothing]

  val root = "https://api.twitch.tv/"

  def parseHttpResponse[Out: Decoder](maybeHttpResponse: Either[TwitchApiException, Response[String]]): F[Out] =
    maybeHttpResponse.map(_.body) match {
      case Right(Right(body)) =>
        parser.parse(body).getOrElse(Json.Null).as[Out] match {
          case Right(resp) =>
            resp.pure
          case Left(DecodingFailure((message, _))) =>
            monadError.raiseError(TwitchParseException(message, body))
        }
      case Right(Left(error)) =>
        println(error)
        parser.parse(error).getOrElse(Json.Null).as[FailureResponse] match {
          case Right(failure) =>
            monadError.raiseError(new TwitchResponseError(failure))
          case Left(DecodingFailure((message, _))) =>
            monadError.raiseError(TwitchParseException(message, error))
        }
      case Left(excp) =>
        monadError.raiseError(TwitchConnectionException(excp))
    }
}
