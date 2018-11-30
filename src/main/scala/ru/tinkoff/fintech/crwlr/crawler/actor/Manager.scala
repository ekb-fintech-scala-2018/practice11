package ru.tinkoff.fintech.crwlr.crawler.actor
import java.util.concurrent.BlockingQueue

import akka.actor.{Actor, ActorRef, Props}
import ru.tinkoff.fintech.crwlr.crawler.Status

import scala.concurrent.Promise
import ru.tinkoff.fintech.crwlr.httpclient._

class Manager(
               wrkFactory: ActorRef => Props,
               monitorQueue: BlockingQueue[Status[Host]],
               result: Promise[Map[Host, Int]]) extends Actor {

  private var referenceCount = Map[Host, Int]()
  private var visitedLinks = Set[Url]()
  private var inProgress = Set[Url]()
  private var workers = Map[Host, ActorRef]()

  override def receive: Receive = {
    case Start(start) =>
      crawlUrl(start)

    case CrawlResult(url, links) =>
      removeFromInProgress(url)

      links.foreach { link =>
        crawlUrl(link)
        referenceCount = referenceCount.updated(link.host, referenceCount.getOrElse(link.host, 0) + 1)
      }

      monitorQueue.offer(Status(referenceCount))

      if (inProgress.isEmpty) {
        result.success(referenceCount)
        context.stop(self)
      }
  }

  private def crawlUrl(url: Url): Unit = {
    if (!visitedLinks.contains(url)) {
      visitedLinks += url
      addToInProgress(url)
      actorFor(url.host) ! Crawl(url)
    }
  }

  private def actorFor(host: Host): ActorRef = {
    workers.get(host) match {
      case None =>
        val workerActor = context.actorOf(wrkFactory(self))
        workers += host -> workerActor
        workerActor

      case Some(ar) => ar
    }
  }

  def addToInProgress(url: Url) = {
    inProgress += url
  }

  def removeFromInProgress(url: Url) = {
    inProgress -= url
  }
}