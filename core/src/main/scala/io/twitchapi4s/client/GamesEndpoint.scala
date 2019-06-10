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

import io.twitchapi4s.model._
import io.twitchapi4s._

object GamesEndpoint {

  implicit val gameDecoder = new Decoder[TwitchGame] {
    final def apply(c: HCursor): Decoder.Result[TwitchGame] =
      for {
        id <- c.downField("id").as[String]
        name <- c.downField("name").as[String]
        boxArtUrl <- c.downField("box_art_url").as[String]
      } yield TwitchGame(id, name, boxArtUrl)
  }
}

trait GamesEndpoint[F[_]] extends TwitchEndpoint[F] {
  import GamesEndpoint._
  import TwitchEndpoint._

  val gamesUrl = s"${root}helix/games"
  val topGamesUrl = s"${root}helix/games/top"

  def getGames(env: TwitchEnv)(ids: List[String], names: List[String]): F[List[TwitchGame]] =
    for {
      request <- sttp.get(uri"$gamesUrl?id=$ids&name=$names").twitchAuth(env).pure
      maybeHttpResponse <- request.send().attempt
      result <- parseHttpResponse[ResponseHolder[List[TwitchGame]]](maybeHttpResponse)
    } yield result.data

  def getGamesR(
    ids: List[String],
    names: List[String]
  )(implicit aa: ApplicativeAsk[F, TwitchEnv]): F[List[TwitchGame]] =
    loadWithApplicativeAsk((env) => getGames(env)(ids, names))

  def getGamesS(
    ids: List[String],
    names: List[String]
  )(implicit ms: MonadState[F, RecoverableTwitchEnv]): F[List[TwitchGame]] =
    loadWithMonadState((env) => getGames(env)(ids, names))


  def getTopGames(
    env: TwitchEnv
  )(
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  ): F[ResponseHolderPage[List[TwitchGame]]] =
    for {
      request <- sttp.get(uri"$topGamesUrl?first=$first&before=$before&after=$after").pure
      maybeHttpResponse <- request.twitchAuth(env).send().attempt
      result <- parseHttpResponse[ResponseHolderPage[List[TwitchGame]]](maybeHttpResponse)
    } yield result

  def getTopGamesR(
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  )(implicit aa: ApplicativeAsk[F, TwitchEnv]): F[ResponseHolderPage[List[TwitchGame]]] =
    loadWithApplicativeAsk((env) => getTopGames(env)(after, before, first))

  def getTopGamesS(
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  )(implicit ms: MonadState[F, RecoverableTwitchEnv]): F[ResponseHolderPage[List[TwitchGame]]] =
    loadWithMonadState((env) => getTopGames(env)(after, before, first))
}
