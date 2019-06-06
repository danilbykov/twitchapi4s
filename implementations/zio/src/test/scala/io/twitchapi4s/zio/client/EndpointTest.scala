package io.twitchapi4s.zio.client

import com.softwaremill.sttp.asynchttpclient.zio.AsyncHttpClientZioBackend
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import scalaz.zio.DefaultRuntime
import scalaz.zio.Ref
import scalaz.zio.interop.catz._
import scalaz.zio.interop.catz.mtl._

import io.twitchapi4s.ClientIdTwitchEnv
import io.twitchapi4s.RecoverableTwitchEnv
import io.twitchapi4s.TwitchEnv
import io.twitchapi4s.WithoutTokenEnv
import io.twitchapi4s.model.GameVideoId
import io.twitchapi4s.model.VideoIds
import io.twitchapi4s.model.UserVideoId
import io.twitchapi4s.zio.client.Endpoint._

class EndpointTest extends WordSpecLike with Matchers {

  val backend = AsyncHttpClientZioBackend()
  val runtime = new DefaultRuntime {}

  val clientId = ???
  val clientSecret = ???
  val env = ClientIdTwitchEnv(clientId)

  val endpoint = new Endpoint[TwitchEnv](backend)
    with GamesEndpoint[TwitchEnv]
    with StreamsEndpoint[TwitchEnv]
    with UsersEndpoint[TwitchEnv]
    with VideosEndpoint[TwitchEnv]

  val recoverableEndpoint = new Endpoint[RecoverableTwitchState](backend)
    with GamesEndpoint[RecoverableTwitchState]

  "A TwitchEndpoint" should {
    "load games/topgames" in {
      runtime.unsafeRunSync((for {
        topGames <- endpoint.getTopGamesR()
        topGame = topGames.data.head
        topGameById <- endpoint.getGamesR(ids = List(topGame.id), names = Nil)
        topGameByName <- endpoint.getGamesR(ids = Nil, names = List(topGame.name))
      } yield {
        topGame shouldBe topGameById.head
        topGame shouldBe topGameByName.head
      }).provide(env)).bimap(throw _, identity)
    }

    "load streams" in {
      runtime.unsafeRunSync((for {
        topGames <- endpoint.getTopGamesR()
        topGame = topGames.data.head
        streams <- endpoint.getStreamsR(communityIds = Nil, gameIds = List(topGame.id),
          languages = Nil, userIds = Nil, userLogins = Nil)
        topStream = streams.data.head
        streamsByLang <- endpoint.getStreamsR(communityIds = Nil, gameIds = Nil,
          languages = List(topStream.language), userIds = Nil, userLogins = Nil)
        streamsByCommunityIds <- endpoint.getStreamsR(communityIds = topStream.communityIds,
          gameIds = Nil, languages = Nil, userIds = Nil, userLogins = Nil)
        streamsByUserId <- endpoint.getStreamsR(communityIds = Nil, gameIds = Nil,
          languages = Nil, userIds = List(topStream.userId), userLogins = Nil)
        streamsByUserLogin <- endpoint.getStreamsR(communityIds = Nil, gameIds = Nil,
          languages = Nil, userIds = Nil, userLogins = List(topStream.userName))
      } yield {
        streamsByLang.data should not be empty
        streamsByCommunityIds.data should not be empty
        streamsByUserId.data should not be empty
        streamsByUserLogin.data should not be empty
      }).provide(env)).bimap(throw _, identity)
    }

    "load videos" in {
      runtime.unsafeRunSync((for {
        topGames <- endpoint.getTopGamesR()
        topGame = topGames.data.head
        videosByGame <- endpoint.getVideosR(GameVideoId(topGame.id))
        video = videosByGame.data.head
        videosByUser <- endpoint.getVideosR(UserVideoId(video.userId))
        videosByIds <- endpoint.getVideosR(VideoIds(List(video.id)), first = 10)
      } yield {
        videosByGame.data should not be empty
        videosByUser.data should not be empty
        videosByIds.data should contain theSameElementsAs List(video)
      }).provide(env)).bimap(throw _, identity)
    }

    "load users" in {
      runtime.unsafeRunSync((for {
        topGames <- endpoint.getTopGamesR()
        topGame = topGames.data.head
        videosByGame <- endpoint.getVideosR(GameVideoId(topGame.id))
        video = videosByGame.data.head
        userById <- endpoint.getUsersR(ids = List(video.userId), logins = Nil)
        userByName <- endpoint.getUsersR(ids = Nil, logins = List(video.userName))
        followers <- endpoint.getFollowsR(fromId = Some(video.userId), toId = None)
      } yield {
        videosByGame.data should not be empty
        userById.head shouldBe userByName.head
        followers.data should not be empty
      }).provide(env)).bimap(throw _, identity)
    }

    "support with RecoverableTwitchEnv" in {
      val ref = runtime.unsafeRunSync(Ref.make[RecoverableTwitchEnv](WithoutTokenEnv(clientId, clientSecret)))
        .toEither
        .right
        .get
      runtime.unsafeRunSync((for {
        starcraft <- recoverableEndpoint.getGamesS(Nil, List("Starcraft"))
        starcraft2 <- recoverableEndpoint.getGamesS(Nil, List("Starcraft 2"))
      } yield {
        starcraft should not be empty
        starcraft2 should not be empty
      }).provide(RecoverableTwitchState(ref))).bimap(throw _, identity)
    }
  }
}
