package ru.tinkoff.fintech.crwlr
import java.util.concurrent.BlockingQueue

import monix.eval.Task
import monix.execution.{AsyncQueue, Scheduler}
import ru.tinkoff.fintech.crwlr.crawler.mnix.MQueue

import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

trait OnFinish[F[_]] {
  def call[A](f: F[A], cb: Either[Throwable, A] => Unit)
}

trait OnNext[F[_]] {
  def next[A <: AnyRef](f: F[A], cb: A => Unit)
}

object OnFinish {
  implicit def fututreOnFinish(implicit ec: ExecutionContext) = new OnFinish[Future] {
    override def call[A](f: Future[A],
                         cb: Either[Throwable, A] => Unit): Unit = {
      f.onComplete(x => cb(x.toEither))
    }
  }

  implicit def taskOnFinish(implicit sc: Scheduler) = new OnFinish[Task] {
    override def call[A](f: Task[A],
                         cb: Either[Throwable, A] => Unit): Unit = {
      f.runAsync(cb)
    }
  }
}

object OnNext {
  implicit def blockingQueueCallback(implicit ec: ExecutionContext) = new OnNext[BlockingQueue] {
    override def next[A <: AnyRef](
      f: BlockingQueue[A], cb: A => Unit): Unit = {

      ec.execute(
        new Runnable {
          @tailrec
          override def run(): Unit = {
            val el = f.take()
            cb(el)
            run()
          }
        }
      )
    }
  }

  implicit def mQueueOnNext(implicit sc: Scheduler) = new OnNext[MQueue] {
    override def next[A <: AnyRef](f: MQueue[A],
                         cb: A => Unit): Unit = {
      f.take
        .foreachL(cb)
        .loopForever
        .runAsyncAndForget
    }
  }
}