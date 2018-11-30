package ru.tinkoff.fintech.crwlr

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO
import cats.instances.future._
import com.typesafe.config.ConfigFactory
import monix.execution.Scheduler
import ru.tinkoff.fintech.crwlr.httpserver.storage.{JobStorage, SlickStorage}
import ru.tinkoff.fintech.crwlr.httpserver.{AkkaServer, Http4sServer, JobManager}
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Server extends App {
  implicit val system = ActorSystem("crwlr")
  implicit val materializer = ActorMaterializer()
  implicit val sc = monix.execution.Scheduler.global

  val config = ConfigFactory.load()
  val crawlerType = config.getString("crawler")

  val launcher = new Launcher()

  implicit val dbExecutor = SlickStorage.dbioToFuture(Database.forConfig("h2jobs"))
  implicit val storage: JobStorage[Future] = SlickStorage.build
  Await.result(dbExecutor(SlickStorage.setup), Duration.Inf)

  val jmFuture = new JobManager[Future](launcher)
  val akkaServer = new AkkaServer(config.getString("server.akka.interface"),
    config.getInt("server.akka.port"), crawlerType, jmFuture)

  val jmIO = new JobManager[IO](launcher)
  val http4Server = new Http4sServer(
    config.getString("server.http4s.interface"), config.getInt("server.http4s.port"), crawlerType, jmIO
  )(Scheduler.io(name = "http4s-server"))
}