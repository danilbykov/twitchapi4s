package io.twitchapi4s.client

import java.time.format.DateTimeFormatter
import java.time.Instant

import scala.language.higherKinds
import scala.util.Try

import cats.MonadError
import cats.mtl.ApplicativeAsk
import cats.mtl.MonadState
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.softwaremill.sttp._
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Json
import io.circe.HCursor
import io.circe.generic.auto._
import io.circe.parser

import io.twitchapi4s._

object TwitchEndpoint {

  implicit class RequestTOps[U[_], T, +S](requestT: RequestT[U, T, S]) {
    def twitchAuth(env: TwitchEnv) =
      env match {
        case ClientIdTwitchEnv(clientId) =>
          requestT.header("Client-ID", clientId)
        case TokenTwitchEnv(token) =>
          requestT.header("Authorization", s"Bearer $token")
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

  implicit val tokenDecoder = new Decoder[Token] {
    final def apply(c: HCursor): Decoder.Result[Token] =
      for {
        accessToken <- c.downField("access_token").as[String]
        expiresIn <- c.downField("expires_in").as[Int]
        tokenType <- c.downField("token_type").as[String]
      } yield Token(accessToken, expiresIn, tokenType)
  }
}

trait TwitchEndpoint[F[_]] {
  import TwitchEndpoint._

  implicit val monadError: MonadError[F, TwitchApiException]
  //implicit val applicativeAsk: ApplicativeAsk[F, TwitchEnv]

  implicit val sttpBackend: SttpBackend[F, Nothing]

  val root = "https://api.twitch.tv/"
  val rootId = "https://id.twitch.tv/"

  def parseHttpResponse[Out: Decoder](maybeHttpResponse: Either[TwitchApiException, Response[String]]): F[Out] =
    maybeHttpResponse.map(r => (r.body, r.headers)) match {
      case Right((Right(body), _)) =>
        parser.parse(body).getOrElse(Json.Null).as[Out] match {
          case Right(resp) =>
            resp.pure
          case Left(DecodingFailure((message, _))) =>
            monadError.raiseError(TwitchParseException(message, body))
        }
      case Right((Left(error), headers)) =>
        headers.find(_._1 == "Ratelimit-Remaining").map(_._2)
          .zip(headers.find(_._1 == "Ratelimit-Reset").map(_._2))
          .headOption match {
            case Some(("0", limit)) =>
              monadError.raiseError(RateLimitExceeded(limit.toLong * 1000))
            case _ =>
              parser.parse(error).getOrElse(Json.Null).as[FailureResponse] match {
                case Right(FailureResponse(Some("Unauthorized"), Some(401), message)) =>
                  monadError.raiseError(UnauthorizedException(message))
                case Right(failure) =>
                  monadError.raiseError(TwitchResponseError(failure))
                case Left(DecodingFailure((message, _))) =>
                  monadError.raiseError(TwitchParseException(message, error))
              }
          }
      case Left(excp) =>
        monadError.raiseError(TwitchConnectionException(excp))
    }

  def getToken(clientId: String, clientSecret: String): F[Token] =
    for {
      request <- monadError.pure(sttp.post(uri"${rootId}oauth2/token?client_id=$clientId&client_secret=$clientSecret&grant_type=client_credentials"))
      maybeHttpResponse <- monadError.attempt(request.send())
      result <- parseHttpResponse[Token](maybeHttpResponse)
    } yield result

  def loadWithApplicativeAsk[R](
    getter: TwitchEnv => F[R]
  )(implicit aa: ApplicativeAsk[F, TwitchEnv]): F[R] =
    for {
      env <- aa.ask
      result <- getter(env)
    } yield result

  def loadWithMonadState[R](
    getter: TwitchEnv => F[R]
  )(implicit ms: MonadState[F, RecoverableTwitchEnv]): F[R] =
    for {
      recoverableEnv <- ms.get
      envAndStatus <-
        recoverableEnv match {
          case e @ ClientIdAndSecretTwitchEnv(_, _, _) =>
            (e, false).pure
          case WithoutTokenEnv(clientId, clientSecret) =>
            getToken(clientId, clientSecret).map { t =>
              (ClientIdAndSecretTwitchEnv(clientId, clientSecret, t.accessToken), true)
            }
        }
      token = envAndStatus._1.token
      newenvAndResult <-
        getter(TokenTwitchEnv(token)) map((envAndStatus._1, _)) recoverWith {
          case _: UnauthorizedException if !envAndStatus._2 =>
            for {
              token <- getToken(recoverableEnv.clientId, recoverableEnv.clientSecret)
              result <- getter(TokenTwitchEnv(token.accessToken))
            } yield (envAndStatus._1.copy(token = token.accessToken), result)
        }
      _ <- ms.set(newenvAndResult._1)
    } yield newenvAndResult._2
}
