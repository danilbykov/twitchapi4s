# Twitch API client on scala
Pure Functional Twitch API client on scala

## Authentication

Twitch API supports two different authentication mechanisms:
- Public application Client ID is used to integrate with the API. This schema provides lowest rate limits.
- Public Client ID and Client Secret are used to generate Application access token. This token is included in all requests
as HTTP header. This schema provides higher rate limit. Token has finite lifecycle and must be recreated after expiration.

For more information refer to: [Twitch apps and authentication guide](https://dev.twitch.tv/docs/authentication/#introduction).

## Usage

First of all you need to define which authentication schema you are going to use. You may authenticate based on Client ID

```scala
val clientId: String = "your-client-id"
val env = io.twitchapi4s.ClientIdTwitchEnv(clientId)
```
or Token
```scala
val token: String = "your-twitch-token"
val env = io.twitchapi4s.TokenTwitchEnv(token)
```

Once environment is created you should create `endpoint` with necessary operations, for example,
```scala
import scalaz.zio.interop.catz._
import scalaz.zio.interop.catz.mtl._
import io.twitchapi4s.zio.client._
import io.twitchapi4s.zio.client.TwitchEndpoint._

val endpoint = new TwitchEndpoint[TwitchEnv](backend)
  with GamesEndpoint[TwitchEnv]
  with StreamsEndpoint[TwitchEnv]
  with UsersEndpoint[TwitchEnv]
  with VideosEndpoint[TwitchEnv]
```
This snapshot and next examples are based on implementation for ZIO but examples for Monix are completely the same except package name.

Now we are ready to execute some queries:
```scala
runtime.unsafeRunSync((for {
  topGames <- endpoint.getTopGamesR()
  topGame = topGames.data.head
  videosByGame <- endpoint.getVideosR(GameVideoId(topGame.id))
  video = videosByGame.data.head
  userById <- endpoint.getUsersR(ids = List(video.userId), logins = Nil)
  followers <- endpoint.getFollowsR(fromId = Some(video.userId), toId = None)
} yield (userById, followers)).provide(env))
```

This approach works fine but have couple drawbacks:
- If you use authentication based on Client ID (`ClientIdTwitchEnv`) then your rate limit is low and you may get
io.twitchapi4s.RateLimitExceded Exception.
- If you use authentication based on Token (`TokenTwitchEnv`) then your token may expire at any time and you have to
generate new one.

To overcome both these issues you may create `endpoint` with another environment
`io.twitchapi4s.zio.client.RecoverableTwitchState`
```scala
import scalaz.zio.interop.catz._
import scalaz.zio.interop.catz.mtl._
import io.twitchapi4s.zio.client._
import io.twitchapi4s.zio.client.TwitchEndpoint._

val recoverableEndpoint = new TwitchEndpoint[RecoverableTwitchState](backend)
  with GamesEndpoint[RecoverableTwitchState]
```

Now to load some data from Twitch you should write something like that
```scala
val env = WithoutTokenEnv(clientId, clientSecret)
val ref = runtime.unsafeRunSync(Ref.make[RecoverableTwitchEnv](env)).toEither.right.get
runtime.unsafeRunSync((for {
  starcraft <- recoverableEndpoint.getGamesS(Nil, List("Starcraft"))
  starcraft2 <- recoverableEndpoint.getGamesS(Nil, List("Starcraft 2"))
} yield (starcraft, starcraft2)).provide(RecoverableTwitchState(ref))
```
This code
- creates mutable `scalaz.zio.Ref` with `WithoutTokenEnv` which contains Client ID and Client Secret
- wraps this `Ref` into `RecoverableTwitchState`
- provides this state in Effect

Client ID and Client Secret will be used to generate token before first request to Twitch. If authentication exception is
thrown at some moment then `recoverableEndpoint` will try to recreate token again before propagation this exception to outer
code.

You may find more examples for ZIO and Monix in classes `io.twitchapi4s.zio.client.TwitchTest` and `io.twitchapi4s.monix.client.TwitchTest`.
