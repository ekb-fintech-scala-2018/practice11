package ru.tinkoff.fintech.crwlr.crawler.actor
import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.util.{Failure, Success}
import ru.tinkoff.fintech.crwlr.httpclient._

class Worker(http: Http, parser: Parsr, master: ActorRef) extends Actor with ActorLogging {
  private var urlsPending: Vector[Url] = Vector.empty
  private var getInProgress = false

  override def receive: Receive = {
    case Crawl(url) =>
      urlsPending = urlsPending :+ url
      startHttpGetIfPossible()

    case HttpGetResult(url, Success(body)) =>
      getInProgress = false
      startHttpGetIfPossible()

      val links = parser.links(url, body)
      master ! CrawlResult(url, links)

    case HttpGetResult(url, Failure(e)) =>
      getInProgress = false
      startHttpGetIfPossible()

      log.error(s"Cannot get contents of $url", e)
      master ! CrawlResult(url, Nil)
  }

  private def startHttpGetIfPossible(): Unit = {
    urlsPending match {
      case url +: tail if !getInProgress =>
        getInProgress = true
        urlsPending = tail

        import context.dispatcher
        http.get(url).onComplete(r => self ! HttpGetResult(url, r))

      case _ =>
    }
  }
}
