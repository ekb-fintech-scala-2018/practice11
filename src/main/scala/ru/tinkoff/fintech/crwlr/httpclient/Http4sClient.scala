package ru.tinkoff.fintech.crwlr.httpclient
import cats.effect.{ContextShift, IO}
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

class Http4sClient(ec: ExecutionContext) extends HttpClient[IO, String] {
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  lazy val bcb = BlazeClientBuilder[IO](ec)

  override def get(url: Url): IO[String] =
    bcb
      .resource
      .use { httpClient =>
        httpClient.expect[String](url.show)
      }
}
