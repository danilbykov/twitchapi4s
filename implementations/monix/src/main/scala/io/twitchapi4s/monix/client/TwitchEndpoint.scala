package io.twitchapi4s.monix.client

import scala.util.control.NonFatal

import cats.Monad
import cats.MonadError
import cats.data.Kleisli
import cats.data.Kleisli._
import cats.effect.concurrent.Ref
import cats.mtl.MonadState

import com.softwaremill.sttp.Request
import com.softwaremill.sttp.Response
import com.softwaremill.sttp.SttpBackend
import monix.eval.Task

import io.twitchapi4s.RecoverableTwitchEnv
import io.twitchapi4s.TwitchApiException
import io.twitchapi4s.TwitchConnectionException
import io.twitchapi4s.{client => baseclient}

trait State[S] {
  def state: Ref[Task, S]
}

object TwitchEndpoint {
  type Effect[R, A] = Kleisli[Task, R, A]

  implicit val twitchTaskMonadError = new MonadError[Task, TwitchApiException] {
    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] =
      fa.flatMap(f)
    override def handleErrorWith[A](fa: Task[A])(f: TwitchApiException => Task[A]): Task[A] =
      fa.onErrorHandleWith({
        case e: TwitchApiException => f(e)
        case e: Throwable => f(TwitchConnectionException(e))
      })
    override def pure[A](x: A): Task[A] =
      Task.pure(x)
    override def raiseError[A](e: TwitchApiException): Task[A] =
      Task.raiseError(e)
    override def tailRecM[A, B](a: A)(f: A => Task[Either[A,B]]): Task[B] =
      Task.tailRecM(a)(f)
  }

  implicit def kleisliTaskMonadState[S, R <: State[S]]: MonadState[Kleisli[Task, R, ?], S] =
    new MonadState[Kleisli[Task, R, ?], S] {
      override val monad: Monad[Kleisli[Task, R, ?]] = implicitly[Monad[Kleisli[Task, R, ?]]]
      override def get: Kleisli[Task, R, S] = Kleisli { r => r.state.get }
      override def set(s: S): Kleisli[Task, R, Unit] = Kleisli { r => r.state.set(s) }
      override def inspect[A](f: S => A): Kleisli[Task, R, A] = Kleisli { r => r.state.get.map(f) }
      override def modify(f: S => S): Kleisli[Task, R, Unit] = Kleisli { r => r.state.update(f) }
    }
}

import TwitchEndpoint._

class TwitchEndpoint[R](
  backend: SttpBackend[Task, Nothing]
) extends baseclient.TwitchEndpoint[Effect[R, ?]] {

  override val monadError = implicitly[MonadError[Effect[R, ?], TwitchApiException]]

  override val sttpBackend = new SttpBackend[Effect[R, ?], Nothing] {
    override def send[T](request: Request[T, Nothing]): Effect[R, Response[T]] = 
      Kleisli.liftF(backend.send(request)
        .onErrorHandleWith({
          case NonFatal(e) => Task.raiseError(TwitchConnectionException(e))
        })
      )
    override def close(): Unit = backend.close
    override def responseMonad = new com.softwaremill.sttp.MonadError[Effect[R, ?]] {
      def flatMap[T, T2](fa: Effect[R, T])(f: T => Effect[R, T2]) =
        fa.flatMap(f)
      def error[T](t: Throwable): Effect[R, T] =
        t match {
          case e: TwitchApiException => Kleisli.liftF(Task.raiseError(e))
          case e: Throwable => Kleisli.liftF(Task.raiseError(TwitchConnectionException(e)))
        }
      protected def handleWrappedError[T](rt: Effect[R, T])(h: PartialFunction[Throwable, Effect[R, T]]) =
        Kleisli { (r: R) =>
          rt.run(r).onErrorRecoverWith(h.andThen(_.run(r)))
        }
      def map[T, T2](fa: Effect[R, T])(f: T => T2) =
        fa.map(f)
      def unit[T](t: T): Effect[R, T] =
        Kleisli.liftF(Task.pure(t))
    }
  }
}

case class RecoverableTwitchState(val state: Ref[Task, RecoverableTwitchEnv]) extends State[RecoverableTwitchEnv]

trait GamesEndpoint[R] extends baseclient.GamesEndpoint[Effect[R, ?]]
trait UsersEndpoint[R] extends baseclient.UsersEndpoint[Effect[R, ?]]
trait StreamsEndpoint[R] extends baseclient.StreamsEndpoint[Effect[R, ?]]
trait VideosEndpoint[R] extends baseclient.VideosEndpoint[Effect[R, ?]]
