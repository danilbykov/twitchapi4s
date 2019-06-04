package io.twitchapi4s.client

import scala.language.higherKinds

import cats.syntax.flatMap._
import cats.syntax.functor._
import com.softwaremill.sttp._
import io.circe._

import io.twitchapi4s.ResponseHolder
import io.twitchapi4s.ResponseHolderPage
import io.twitchapi4s.model._

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

trait GamesEndpoint[F[_]] extends Endpoint[F] {
  import GamesEndpoint._
  import Endpoint._

  val gamesUrl = s"${root}helix/games"
  val topGamesUrl = s"${root}helix/games/top"

  def getGames(ids: List[String], names: List[String]): F[List[TwitchGame]] =
    for {
      env <- applicativeAsk.ask
      request = sttp.get(uri"$gamesUrl?id=$ids&name=$names").twitchAuth(env)
      maybeHttpResponse <- monadError.attempt(request.send())
      result <- parseHttpResponse[ResponseHolder[List[TwitchGame]]](maybeHttpResponse)
    } yield result.data

  def getTopGames(
    after: Option[String] = None,
    before: Option[String] = None,
    first: Int = 20
  ): F[ResponseHolderPage[List[TwitchGame]]] =
    for {
      env <- applicativeAsk.ask
      request = sttp.get(uri"$topGamesUrl?first=$first&before=$before&after=$after")
      maybeHttpResponse <- monadError.attempt(request.twitchAuth(env).send())
      result <- parseHttpResponse[ResponseHolderPage[List[TwitchGame]]](maybeHttpResponse)
    } yield result
}
