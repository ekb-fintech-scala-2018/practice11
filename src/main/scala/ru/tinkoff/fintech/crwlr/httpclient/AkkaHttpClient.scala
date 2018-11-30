package ru.tinkoff.fintech.crwlr.httpclient
import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.Future

class AkkaHttpClient(implicit val system: ActorSystem, materializer: ActorMaterializer) extends HttpClient[Future, String] {
  import system.dispatcher

  override def get(url: Url): Future[String] = {
    for {
      res <- Http().singleRequest(HttpRequest(uri = url.show))
      body <- res.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield
      body.decodeString(
        res.entity.contentType.charsetOption
          .map(ch => ch.nioCharset())
          .getOrElse(StandardCharsets.UTF_8)
      )
  }
}