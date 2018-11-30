package ru.tinkoff.fintech.crwlr.crawler

import monix.eval.{Fiber, Task}
import monix.execution.{AsyncQueue, Scheduler}
import ru.tinkoff.fintech.crwlr.httpclient._

package object mnix {
  type Host = String
  type Path = String
  type Body = String

  type Http = HttpClient[Task,Body]
  type Parsr = Parser[Body]

  def url(host: Host, path: Path) = Url(host, Some(path))


  case class WorkerData(queue: MQueue[Url], fiber: Fiber[Unit])
  case class CrawlerData(referenceCount: Map[Host, Int], visitedLinks: Set[Url], inProgress: Set[Url], workers: Map[Host, WorkerData])

  sealed trait CrawlerMessage

  /**
    * Start the crawling process for the given URL. Should be sent only once.
    */
  case class Start(url: Url) extends CrawlerMessage
  case class CrawlResult(url: Url, links: List[Url]) extends CrawlerMessage

  class MQueue[T](q: AsyncQueue[T]) {
    def take: Task[T] = {
      Task.deferFuture(q.poll())
    }
    def offer(t: T): Task[Unit] = {
      Task.eval(q.offer(t))
    }
  }
  object MQueue {
    def make[T](implicit scheduler: Scheduler): MQueue[T] = new MQueue(AsyncQueue.bounded(16))
  }
}