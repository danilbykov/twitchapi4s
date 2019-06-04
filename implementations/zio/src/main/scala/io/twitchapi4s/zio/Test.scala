package io.twitchapi4s.zio

    import com.softwaremill.sttp.asynchttpclient.zio.AsyncHttpClientZioBackend
    import io.twitchapi4s.zio.client.Endpoint
    import io.twitchapi4s.zio.client.GamesEndpoint
    import io.twitchapi4s.zio.client.StreamsEndpoint
    import io.twitchapi4s.zio.client.UsersEndpoint
  import scalaz.zio.DefaultRuntime
    import io.twitchapi4s.ClientTwitchEnv

object Test {

  val runtime = new DefaultRuntime {}

  def main(args: Array[String]): Unit = {
    val backend = AsyncHttpClientZioBackend()
    val endpoint = new Endpoint(backend) with UsersEndpoint with GamesEndpoint with StreamsEndpoint

    val env = ClientTwitchEnv("yms3qywhglkgoq9qawhx11taiqv0tk")

    //val usersZio = endpoint.getUsers()//logins = List("Alex007SC2"))
    //val logins = (1 to 150).map(_.toString).toList
    //val usersZio = endpoint.getUsers(logins = logins, ids = Nil)
    //val usersZio = endpoint.getUsers(logins = List("Alex007SC2"))
    //val users = runtime.unsafeRunSync(usersZio.provide(env))
    //println(users)

    //val follows = runtime.unsafeRunSync(endpoint.getFollows(None, Some("86948541")).provide(env))
    //println(follows.map(_.data.mkString("\n")))

    //val games = runtime.unsafeRunSync(endpoint.getGames(Nil, List("Starcraft")).provide(env))
    //println(games.map(_.mkString("\n")))
    //val topGames = runtime.unsafeRunSync(endpoint.getTopGames().provide(env))
    //println(topGames.map(_.data.mkString("\n")))

    val streams = runtime.unsafeRunSync(endpoint.getStreams(Nil, Nil, Nil, Nil, List("chocoTaco")).provide(env))
    println(streams.map(_.data.mkString("\n")))
  }
}
