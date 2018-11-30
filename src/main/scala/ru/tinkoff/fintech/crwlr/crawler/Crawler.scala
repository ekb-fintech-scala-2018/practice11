package ru.tinkoff.fintech.crwlr.crawler

import akka.actor.ActorSystem

import scala.concurrent.Future
import monix.eval.Task
import java.util.concurrent.BlockingQueue

import ru.tinkoff.fintech.crwlr.FunK
import ru.tinkoff.fintech.crwlr.crawler.mnix.MQueue
import ru.tinkoff.fintech.crwlr.httpclient._

final case class CrawlReult[Host](values: Map[Host, Int])
final case class Status[Host](values: Map[Host, Int])

trait Crawler[M[_],F[_],Host,Path] {
  def crawl(startUrl: Url): (M[Status[Host]], F[CrawlReult[Host]])
}

object Crawler {


  def url(host: String, path: String) = Url(host, Some(path))

  def akka[CF[_]](
      httpClient: HttpClient[CF,String],
      parser: Parser[String]
    )(implicit
      funK: FunK[CF, Future],
      actorSystem: ActorSystem): Crawler[BlockingQueue, Future, String, String] =
    actor.Crwlr(clientConv(httpClient), parser)(actorSystem)

  def monix[CF[_]](
      httpClient: HttpClient[CF,String],
      parser: Parser[String]
    )(implicit
      funK: FunK[CF, Task]): Crawler[MQueue, Task, String, String] =
    mnix.Crwlr(clientConv(httpClient), parser)


  def clientConv[H[_],G[_],B](client: HttpClient[H,B])(
    implicit funK: FunK[H,G]): HttpClient[G,B] = new HttpClient[G,B] {
      override def get(url: Url): G[B] = funK(client.get(url))
  }

}
