package ru.tinkoff.fintech.crwlr.httpserver
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe._
import org.http4s.implicits._
import io.circe.syntax._
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext

class Http4sServer(
                    httpInterface: String,
                    httpPort: Int,
                    crawlerType: String,
                    jb: JobManager
                  )(implicit ec: ExecutionContext) {

  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)

  implicit val decoder = jsonOf[IO, AddJob]

  val routes = HttpRoutes.of[IO] {
    case req @ GET -> Root / "jobs" => Ok(jb.snapshot.asJson)
    case req @ POST -> Root / "jobs" =>
      for {
        // Decode a User request
        addJob <- req.as[AddJob]
        // Encode a hello response
        resp <- Ok(jb.create(crawlerType, addJob))
      } yield resp
  }.orNotFound

  val server = BlazeServerBuilder[IO]
    .bindHttp(httpPort, httpInterface)
    .withHttpApp(routes)
    .withExecutionContext(ec)
    .resource

  val fiber = server.use{_ =>
    println(s"'http4s' service HTTP server started on ${httpInterface} ${httpPort}")
    IO.never
  }.start.unsafeRunSync()
}
