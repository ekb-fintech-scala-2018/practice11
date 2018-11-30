package ru.tinkoff.fintech.crwlr.crawler.actor
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import ru.tinkoff.fintech.crwlr.crawler.{CrawlReult, Crawler, Status}
import ru.tinkoff.fintech.crwlr.httpclient.{Parser, _}

import scala.concurrent.{Future, Promise}

object Crwlr {
  def apply(
      httpClient: HttpClient[Future,String],
      parser: Parser[String]
    )(implicit actorSystem: ActorSystem): Crawler[BlockingQueue, Future, String, String] = {
    import actorSystem.dispatcher
    implicit val materializer = ActorMaterializer()

    val akkaHttpClient = new AkkaHttpClient()

    def wrkFactory(manager: ActorRef): Props =
      Props(new Worker(
        httpClient,
        parser,
        manager
      ))

    val result = Promise[Map[String, Int]]()
    val monQueue = new LinkedBlockingQueue[Status[String]]()
    val manager = actorSystem.actorOf(Props(new Manager(wrkFactory, monQueue, result)))

    startUrl: Url => {
      manager ! Start(startUrl)

      (monQueue, result.future.map(r => CrawlReult(r)))
    }
  }
}