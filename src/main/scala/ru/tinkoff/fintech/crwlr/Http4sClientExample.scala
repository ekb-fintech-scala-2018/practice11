package ru.tinkoff.fintech.crwlr

import cats.effect.{ContextShift, IO, Timer}
import ru.tinkoff.fintech.crwlr.httpclient.{StringParser, Url}
import org.http4s.client.blaze._
import org.http4s.client._

import scala.concurrent.ExecutionContext.global

object Http4sClientExample extends App {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  val url = Url("akka.io", None, "https", None, None)

  BlazeClientBuilder[IO](global).resource.use { httpClient =>
    httpClient.expect[String](url.show)
  }

  val res = for {
    page <- BlazeClientBuilder[IO](global).resource.use { httpClient =>
              httpClient.expect[String](url.show)
            }
  } yield StringParser.links(url, page)

  println(res.unsafeRunSync())
}
