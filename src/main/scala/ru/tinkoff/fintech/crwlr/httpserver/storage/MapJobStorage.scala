package ru.tinkoff.fintech.crwlr.httpserver.storage

import cats.Id
import ru.tinkoff.fintech.crwlr.FunK
import ru.tinkoff.fintech.crwlr.httpserver.JobStatus

import scala.collection.concurrent.TrieMap

class MapJobStorage extends JobStorage[Id] {
  private val jobsMap = TrieMap.empty[String, JobStatus]

  override def snapshot(): Id[Map[String, JobStatus]] =
    jobsMap.snapshot.toMap

  override def updateJob(key: String, status: JobStatus): Id[Unit] =
    jobsMap.put(key, status)
}

object MapJobStorage {
  def build[F[_]](implicit exec: FunK[Id, F]): JobStorage[F] =
    JobStorage.convertedStorage(new MapJobStorage, exec)
}


