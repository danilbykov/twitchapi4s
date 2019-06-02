package io.twitchapi4s.zio.client

import io.twitchapi4s.{client => baseclient}
import scalaz.zio.ZIO
import scalaz.zio.IO
import io.twitchapi4s.TwitchEnv
import com.softwaremill.sttp.SttpBackend
import cats.MonadError
import cats.mtl.ApplicativeAsk
import scalaz.zio.interop.catz._
import scalaz.zio.interop.catz.mtl._
import io.twitchapi4s.TwitchApiException
    import com.softwaremill.sttp.Response
    import com.softwaremill.sttp.Request
    import io.twitchapi4s.TwitchConnectionException
import scala.util.control.NonFatal

object Endpoint {
  type Effect[A] = ZIO[TwitchEnv, TwitchApiException, A]

  type IOThrowable[A] = ZIO[Any, Throwable, A]
}

import Endpoint._
class Endpoint(
  backend: SttpBackend[IOThrowable, Nothing]
) extends baseclient.Endpoint[Effect] {

  override val monadError = implicitly[MonadError[Effect, TwitchApiException]]

  override val applicaveAsk = implicitly[ApplicativeAsk[Effect, TwitchEnv]]


  override val sttpBackend = new SttpBackend[Effect, Nothing] {
    override def send[T](request: Request[T, Nothing]): Effect[Response[T]] =
      backend.send(request).catchAll({
        case NonFatal(e) => ZIO.fail(TwitchConnectionException(e))
      })
    override def close(): Unit = backend.close
    override def responseMonad = new com.softwaremill.sttp.MonadError[Effect] {
      def flatMap[T, T2](fa: Effect[T])(f: T => Effect[T2]) =
        fa.flatMap(f)
      def error[T](t: Throwable): Effect[T] =
        ZIO.fail(t match {
          case e: TwitchApiException => e
          case e: Throwable => TwitchConnectionException(e)
        })
      protected def handleWrappedError[T](rt: Effect[T])(h: PartialFunction[Throwable, Effect[T]]): Effect[T] =
        rt.catchAll(h)
      def map[T, T2](fa: Effect[T])(f: T => T2) =
        fa.map(f)
      def unit[T](t: T): Effect[T] =
        ZIO.succeed(t)
    }
  }
}
