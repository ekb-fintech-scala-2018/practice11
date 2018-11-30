package ru.tinkoff.fintech.crwlr.httpserver
import java.util.UUID

import cats.Functor
import cats.syntax.functor._
import ru.tinkoff.fintech.crwlr.httpclient.Url
import ru.tinkoff.fintech.crwlr.httpserver.storage.JobStorage
import ru.tinkoff.fintech.crwlr.{Launcher, RunAsync, ServerProgram}

class JobManager[F[_] :RunAsync :Functor](launcher: Launcher)(implicit jobStorage: JobStorage[F]) {

  def snapshot: F[Map[String, JobStatus]] = jobStorage.snapshot()

  def create(crawlerType: String, addJob: AddJob): F[String] = {
    val jobKey = UUID.randomUUID().toString

    launcher.launch(
      crawlerType,
      Url(proto = addJob.proto, host = addJob.host),
      ServerProgram(
        st => RunAsync[F].runAsync(jobStorage.updateJob(jobKey, JobStatus(isCompleted = false, st.values))),
        res => RunAsync[F].runAsync(jobStorage.updateJob(jobKey, JobStatus(isCompleted = true, res.values)))
      )
    )

    jobStorage.updateJob(jobKey, JobStatus(isCompleted = false, Map.empty)).map(_ => jobKey)
  }
}