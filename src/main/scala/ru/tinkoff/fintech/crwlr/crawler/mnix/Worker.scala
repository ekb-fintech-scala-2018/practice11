package ru.tinkoff.fintech.crwlr.crawler.mnix

import monix.eval.{Fiber, Task}
import ru.tinkoff.fintech.crwlr.httpclient._

trait Worker {
  def http: Http
  def parseLinks: Parsr

  def worker(workerQueue: MQueue[Url], crawlerQueue: MQueue[CrawlerMessage]): Task[Fiber[Unit]] = {
    def handleUrl(url: Url): Task[Unit] = {
      http
        .get(url)
        .attempt
        .map {
          case Left(t) =>
            println(s"Cannot get contents of $url", t)
            List.empty[Url]
          case Right(b) => parseLinks.links(url, b)
        }
        .flatMap(r => crawlerQueue.offer(CrawlResult(url, r)))
    }

    workerQueue.take
      .flatMap(handleUrl)
      .restartUntil(_ => false)
      .start
  }
}
