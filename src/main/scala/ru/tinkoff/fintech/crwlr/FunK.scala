package ru.tinkoff.fintech.crwlr
import cats.effect.IO
import monix.eval.Task

import scala.concurrent.Future

trait FunK[H[_], G[_]] {
  def apply[A](h: =>H[A]): G[A]
}

object FunK {
  implicit def identity[H[_]] = new FunK[H,H] {
    override def apply[A](h: =>H[A]): H[A] = h
  }

  implicit def future2task = new FunK[Future,Task] {
    override def apply[A](h: =>Future[A]): Task[A] = Task.deferFuture(h)
  }

  implicit def io2future = new FunK[IO,Future] {
    override def apply[A](h: => IO[A]): Future[A] = h.unsafeToFuture()
  }

  implicit def io2task = new FunK[IO,Task] {
    override def apply[A](h: => IO[A]): Task[A] =
      Task.async { cb =>
        h.unsafeRunAsync {
          case err@ Left(_) => cb(err)
          case succ@ Right(_) => cb(succ)
        }
      }
  }
}