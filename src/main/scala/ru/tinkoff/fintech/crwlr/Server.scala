package ru.tinkoff.fintech.crwlr
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import monix.execution.Scheduler
import ru.tinkoff.fintech.crwlr.httpserver.{AkkaServer, Http4sServer, JobManager}

object Server extends App {
  implicit val system = ActorSystem("crwlr")
  implicit val materializer = ActorMaterializer()
  implicit val sc = monix.execution.Scheduler.global

  val config = ConfigFactory.load()
  val crawlerType = config.getString("crawler")

  val jb = new JobManager
  val akkaServer = new AkkaServer(config.getString("server.akka.interface"), config.getInt("server.akka.port"), crawlerType, jb)
  val http4Server = new Http4sServer(
    config.getString("server.http4s.interface"), config.getInt("server.http4s.port"), crawlerType, jb
  )(Scheduler.io(name = "http4s-server"))
}