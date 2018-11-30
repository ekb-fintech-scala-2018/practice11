package ru.tinkoff.fintech.crwlr
import java.util.concurrent.BlockingQueue

import monix.eval.Task
import monix.execution.Scheduler
import ru.tinkoff.fintech.crwlr.crawler.mnix.MQueue

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

trait OnFinish[F[_]] {
  def call[A](f: F[A])(cb: Either[Throwable, A] => Unit)
}

trait OnNext[F[_]] {
  def next[A](f: F[A])(cb: A => Unit)
}

object OnFinish {
  def apply[F[_] :OnFinish] : OnFinish[F] = implicitly

  implicit def futureOnFinish(implicit ec: ExecutionContext): OnFinish[Future] = new OnFinish[Future] {
    override def call[A](f: Future[A]
                        )(cb: Either[Throwable, A] => Unit): Unit = {
      f.onComplete(x => cb(x.toEither))
    }
  }

  implicit def taskOnFinish(implicit sc: Scheduler): OnFinish[Task] = new OnFinish[Task] {
    override def call[A](f: Task[A]
                        )(cb: Either[Throwable, A] => Unit): Unit = {
      f.runAsync(cb)
    }
  }
}

object OnNext {
  def apply[F[_] :OnNext] : OnNext[F] = implicitly

  implicit def blockingQueueCallback(implicit ec: ExecutionContext): OnNext[BlockingQueue] = new OnNext[BlockingQueue] {
    override def next[A](
      f: BlockingQueue[A])(cb: A => Unit): Unit = {

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

  implicit def mQueueOnNext(implicit sc: Scheduler): OnNext[MQueue] = new OnNext[MQueue] {
    override def next[A](f: MQueue[A]
                        )(cb: A => Unit): Unit = {
      f.take
        .foreachL(cb)
        .loopForever
        .runAsyncAndForget
    }
  }
}