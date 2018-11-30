package ru.tinkoff.fintech.crwlr.httpserver
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import monix.execution.Scheduler
import ru.tinkoff.fintech.crwlr.httpclient.Url
import ru.tinkoff.fintech.crwlr.{Launcher, ServerProgram}

import scala.collection.concurrent.TrieMap

class JobManager(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer, scheduler: Scheduler) {
  private val jobsMap = TrieMap.empty[String,JobStatus]

  def snapshot = jobsMap.snapshot()
  def create(crawlerType: String, addJob: AddJob): String = {
    val jobKey = UUID.randomUUID().toString

    Launcher.launch(
      crawlerType,
      Url(proto = addJob.proto, host = addJob.host),
      ServerProgram(
        st => {
          jobsMap.put(jobKey, JobStatus(isCompleted = false, st.values))
        },
        res => {
          jobsMap.put(jobKey, JobStatus(isCompleted = true, res.values))
        }
      )
    )

    jobKey
  }
}