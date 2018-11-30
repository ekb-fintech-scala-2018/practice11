package ru.tinkoff.fintech.crwlr

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import ru.tinkoff.fintech.crwlr.httpclient._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AkkaHttpClientExample extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val url = Url("akka.io", None, "https", None, None)

  val responseFuture: Future[HttpResponse] =
    Http().singleRequest(HttpRequest(uri = url.show))

  responseFuture
    .onComplete {
      case Success(res) =>
        println(res)
        res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
          val decoded = body.decodeString(
            res.entity.contentType.charsetOption
              .map(ch => ch.nioCharset())
              .getOrElse(StandardCharsets.UTF_8)
          )
          println(decoded)
          println(
            httpclient.StringParser.links(
              url,
              decoded
            ).map(_.show)
          )
        }
      case Failure(_) => sys.error("something wrong")
    }
}
