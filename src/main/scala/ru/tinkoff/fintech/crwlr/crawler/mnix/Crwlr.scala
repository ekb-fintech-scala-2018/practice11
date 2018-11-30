package ru.tinkoff.fintech.crwlr.crawler.mnix

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import ru.tinkoff.fintech.crwlr.crawler.{CrawlReult, Crawler, Status}
import ru.tinkoff.fintech.crwlr.httpclient._

import scala.concurrent.Future



object Crwlr extends App {
  def apply(
    httpClient: HttpClient[Task,String],
    parser: Parser[String]
  ): Crawler[MQueue, Task, String, String] = {
    new Crawler[MQueue, Task, String, String] {
      override def crawl(
        startUrl: Url): (MQueue[Status[String]], Task[CrawlReult[String]]) = {

        val (mon, task) = new CrawlRoutines(httpClient, parser).crawl(startUrl)
        (
          mon,
          task.map(CrawlReult(_))
        )
      }
    }
  }
}

class CrawlRoutines(
  val http: Http,
  val parseLinks: Parsr) extends Worker with Manager {

  def crawl(crawlUrl: Url): (MQueue[Status[Host]], Task[Map[Host, Int]]) = {
    val crawlerQueue = MQueue.make[CrawlerMessage]
    val monQueue = MQueue.make[Status[Host]]
    (
      monQueue,
      for {
        _ <- crawlerQueue.offer(Start(crawlUrl))
        r <- crawler(crawlerQueue, monQueue, CrawlerData(Map(), Set(), Set(), Map()))
      } yield r
    )
  }
}