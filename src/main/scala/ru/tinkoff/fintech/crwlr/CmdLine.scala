package ru.tinkoff.fintech.crwlr
import java.net.URI

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import ru.tinkoff.fintech.crwlr.httpclient.Url

object CmdLine extends App {
  implicit val system = ActorSystem("crwlr")
  implicit val materializer = ActorMaterializer()
  implicit val sc = monix.execution.Scheduler.global

  if (args.length == 1) {
    val config = ConfigFactory.load()
    val uri = new URI(args(0))
    val crawlerType = config.getString("crawler")
    Launcher.launch(
      crawlerType,
      Url(host = uri.getHost, path = Some(uri.getPath), proto = uri.getScheme),
      Program0
    )
  } else {
    println("bad args")
  }
}
