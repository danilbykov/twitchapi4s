package io.twitchapi4s.monix.client

import cats.effect.concurrent.Ref
import cats.mtl.implicits._
import com.softwaremill.sttp.asynchttpclient.monix.AsyncHttpClientMonixBackend
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import io.twitchapi4s.ClientIdTwitchEnv
import io.twitchapi4s.RecoverableTwitchEnv
import io.twitchapi4s.TwitchEnv
import io.twitchapi4s.WithoutTokenEnv
import io.twitchapi4s.monix.client.TwitchEndpoint._

class TwitchTest extends WordSpecLike with Matchers {

  val backend = AsyncHttpClientMonixBackend()

  val clientId = ???
  val clientSecret = ???
  val env = ClientIdTwitchEnv(clientId)
  val env2 = WithoutTokenEnv(clientId, clientSecret)

  val endpoint = new TwitchEndpoint[TwitchEnv](backend)
    with GamesEndpoint[TwitchEnv]

  val recoverableEndpoint = new TwitchEndpoint[RecoverableTwitchState](backend)
    with GamesEndpoint[RecoverableTwitchState]

  "A TwitchTest" should {
    "load games/topgames" in {
      (for {
        topGames <- endpoint.getTopGamesR()
        topGame = topGames.data.head
        topGameById <- endpoint.getGamesR(ids = List(topGame.id), names = Nil)
        topGameByName <- endpoint.getGamesR(ids = Nil, names = List(topGame.name))
      } yield {
        topGame shouldBe topGameById.head
        topGame shouldBe topGameByName.head
      }).run(env).runSyncUnsafe()
    }

    "support with RecoverableTwitchEnv" in {
      val state = RecoverableTwitchState(Ref.unsafe[Task, RecoverableTwitchEnv](env2))
      (for {
        starcraft <- recoverableEndpoint.getGamesS(Nil, List("Starcraft"))
        starcraft2 <- recoverableEndpoint.getGamesS(Nil, List("Starcraft 2"))
      } yield {
        starcraft should not be empty
        starcraft2 should not be empty
        println(starcraft.mkString("\n"))
        println(starcraft2.mkString("\n"))
      }).run(state).runSyncUnsafe()
    }
  }
}
