package io.twitchapi4s.zio

    import com.softwaremill.sttp.asynchttpclient.zio.AsyncHttpClientZioBackend
    import io.twitchapi4s.zio.client.Endpoint
    import io.twitchapi4s.zio.client.UsersEndpoint
  import scalaz.zio.DefaultRuntime
    import io.twitchapi4s.ClientTwitchEnv

object Test {

  val runtime = new DefaultRuntime {}

  def main(args: Array[String]): Unit = {
    val backend = AsyncHttpClientZioBackend()
    val endpoint = new Endpoint(backend) with UsersEndpoint

    val env = ClientTwitchEnv("client-id")

    //val usersZio = endpoint.getUsers()//logins = List("Alex007SC2"))
    //val logins = (1 to 150).map(_.toString).toList
    //val usersZio = endpoint.getUsers(logins = logins, ids = Nil)
    //val usersZio = endpoint.getUsers(logins = List("Alex007SC2"))
    //val users = runtime.unsafeRunSync(usersZio.provide(env))
    //println(users)


    val follows = runtime.unsafeRunSync(endpoint.getFollows(None, Some("86948541")).provide(env))
    println(follows.map(_.data.mkString("\n")))
  }
}
