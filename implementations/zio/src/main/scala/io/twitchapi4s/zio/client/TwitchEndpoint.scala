package io.twitchapi4s.zio.client

import scala.util.control.NonFatal

import cats.Monad
import cats.MonadError
import cats.mtl.MonadState
import com.softwaremill.sttp.Response
import com.softwaremill.sttp.Request
import com.softwaremill.sttp.SttpBackend
import scalaz.zio.Ref
import scalaz.zio.Task
import scalaz.zio.ZIO
import scalaz.zio.interop.catz._

import io.twitchapi4s.RecoverableTwitchEnv
import io.twitchapi4s.TwitchApiException
import io.twitchapi4s.TwitchConnectionException
import io.twitchapi4s.{client => baseclient}

trait State[S] {
  def state: Ref[S]
}

object TwitchEndpoint {
  type Effect[R, A] = ZIO[R, TwitchApiException, A]

  implicit def zioMonadState[S, R <: State[S], E]: MonadState[ZIO[R, E, ?], S] =
    new MonadState[ZIO[R, E, ?], S] {
      val monad: Monad[ZIO[R, E, ?]] = implicitly[Monad[ZIO[R, E, ?]]]
      def get: ZIO[R, E, S] = ZIO.accessM(_.state.get)
      def set(s: S): ZIO[R, E, Unit] = ZIO.accessM(_.state.set(s).unit)
      def inspect[A](f: S => A): ZIO[R, E, A] = ZIO.accessM(_.state.get.map(f))
      def modify(f: S => S): ZIO[R, E, Unit] = ZIO.accessM(_.state.update(f).unit)
    }
}

import TwitchEndpoint._
class TwitchEndpoint[R](
  backend: SttpBackend[Task, Nothing]
) extends baseclient.TwitchEndpoint[Effect[R, ?]] {

  override val monadError = implicitly[MonadError[Effect[R, ?], TwitchApiException]]

  override val sttpBackend = new SttpBackend[Effect[R, ?], Nothing] {
    override def send[T](request: Request[T, Nothing]): Effect[R, Response[T]] =
      backend.send(request).catchAll({
        case NonFatal(e) => ZIO.fail(TwitchConnectionException(e))
      })
    override def close(): Unit = backend.close
    override def responseMonad = new com.softwaremill.sttp.MonadError[Effect[R, ?]] {
      def flatMap[T, T2](fa: Effect[R, T])(f: T => Effect[R, T2]) =
        fa.flatMap(f)
      def error[T](t: Throwable): Effect[R, T] =
        ZIO.fail(t match {
          case e: TwitchApiException => e
          case e: Throwable => TwitchConnectionException(e)
        })
      protected def handleWrappedError[T](rt: Effect[R, T])(h: PartialFunction[Throwable, Effect[R, T]]) =
        rt.catchSome(h)
      def map[T, T2](fa: Effect[R, T])(f: T => T2) =
        fa.map(f)
      def unit[T](t: T): Effect[R, T] =
        ZIO.succeed(t)
    }
  }
}

case class RecoverableTwitchState(val state: Ref[RecoverableTwitchEnv]) extends State[RecoverableTwitchEnv]

trait GamesEndpoint[R] extends baseclient.GamesEndpoint[Effect[R, ?]]
trait UsersEndpoint[R] extends baseclient.UsersEndpoint[Effect[R, ?]]
trait StreamsEndpoint[R] extends baseclient.StreamsEndpoint[Effect[R, ?]]
trait VideosEndpoint[R] extends baseclient.VideosEndpoint[Effect[R, ?]]
