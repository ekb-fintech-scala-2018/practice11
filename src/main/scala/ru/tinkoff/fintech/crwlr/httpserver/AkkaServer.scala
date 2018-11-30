package ru.tinkoff.fintech.crwlr.httpserver

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer
import monix.execution.Scheduler
import ru.tinkoff.fintech.crwlr.{Launcher, ServerProgram}
import ru.tinkoff.fintech.crwlr.httpclient.Url

import scala.collection.concurrent.TrieMap
import scala.util.{Failure, Success}

class AkkaServer(
                  httpInterface: String,
                  httpPort: Int,
                  crawlerType: String,
                  jb: JobManager
  )(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer, scheduler: Scheduler) extends Directives {

  import AkkaHttpCirceSupport._

  lazy val routes: Route =
      path("jobs") {
        pathEndOrSingleSlash {
          get {
            complete(jb.snapshot)
          }
        }
      } ~ path("jobs") {
        pathEndOrSingleSlash {
          post {
            entity(as[AddJob]) { addJob =>
              val jobKey = UUID.randomUUID().toString
              complete(jb.create(crawlerType, addJob))
            }
          }
        }
      }

  Http()
      .bindAndHandle(routes, httpInterface, httpPort)
    .andThen {
      case Success(binding) =>
        println(
          s"'akka' service HTTP server started on ${binding.localAddress}")
      case Failure(cause) =>
        println(
          s"cannot start 'akka' HTTP server on $httpInterface:$httpPort",
          cause)
    }
}