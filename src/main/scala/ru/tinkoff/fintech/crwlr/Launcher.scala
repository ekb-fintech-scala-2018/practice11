package ru.tinkoff.fintech.crwlr

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import monix.execution.Scheduler
import ru.tinkoff.fintech.crwlr.crawler.Crawler
import ru.tinkoff.fintech.crwlr.httpclient._

object Launcher {
  def launch(
    crawlerType: String,
    url: Url,
    program: Program)(
    implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer, scheduler: Scheduler) = {
    crawlerType match {
      case "akka/akka" =>

        val crwlr = Crawler.akka(new AkkaHttpClient(), StringParser)
        program.run(crwlr, url)

      case "monix/akka" =>

        val crwlr = Crawler.monix(new AkkaHttpClient(), StringParser)
        program.run(crwlr, url)

      case "akka/http4s" =>

        val crwlr = Crawler.akka(new Http4sClient(Scheduler.io("http4s-client")), StringParser)
        program.run(crwlr, url)

      case "monix/http4s" =>

        val crwlr = Crawler.monix(new Http4sClient(Scheduler.io("http4s-client")), StringParser)
        program.run(crwlr, url)
    }
  }
}