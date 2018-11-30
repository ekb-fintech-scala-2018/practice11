package ru.tinkoff.fintech.crwlr.crawler
import ru.tinkoff.fintech.crwlr.httpclient._

import scala.concurrent.Future
import scala.util.Try

package object actor {
  type Host = String
  type Path = String
  type Body = String

  type Http = HttpClient[Future,Body]
  type Parsr = Parser[Body]

  def url(host: Host, path: Path) = Url(host, Some(path))

  sealed trait ManagerMessage

  /**
    * Start the crawling process for the given URL. Should be sent only once.
    */
  case class Start(url: Url) extends ManagerMessage
  case class CrawlResult(url: Url, links: List[Url]) extends ManagerMessage

  sealed trait WorkerMessage
  case class Crawl(url: Url) extends WorkerMessage
  case class HttpGetResult(url: Url, result: Try[Body]) extends WorkerMessage
}