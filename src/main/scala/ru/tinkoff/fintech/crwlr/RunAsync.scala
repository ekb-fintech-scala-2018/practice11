package ru.tinkoff.fintech.crwlr

import cats.effect.IO

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait RunAsync[F[_]] {
  def runAsync(f: F[_]): Unit
}

object RunAsync {
  import scala.concurrent.ExecutionContext.Implicits.global

  def apply[F[_] :RunAsync] : RunAsync[F] = implicitly

  implicit val futureRunAsync: RunAsync[Future] = new RunAsync[Future] {
    override def runAsync(f: Future[_]): Unit = {
      f.onComplete {
        case Failure(th) => th.printStackTrace()
        case Success(_) =>
      }
    }
  }

  implicit val ioRunAsync: RunAsync[IO] = new RunAsync[IO] {
    override def runAsync(f: IO[_]): Unit = f.unsafeRunAsync {
      case Left(th) => th.printStackTrace()
      case Right(_) =>
    }
  }
}
