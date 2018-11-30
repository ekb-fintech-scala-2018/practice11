package ru.tinkoff.fintech.crwlr.httpserver.storage

import ru.tinkoff.fintech.crwlr.FunK
import ru.tinkoff.fintech.crwlr.httpserver.JobStatus

trait JobStorage[F[_]] {
  def snapshot(): F[Map[String, JobStatus]]

  def updateJob(key: String, status: JobStatus): F[Unit]
}

object JobStorage {
  implicit def convertedStorage[F[_], G[_]](implicit storageF: JobStorage[F], funK: FunK[F, G]): JobStorage[G] =
    new JobStorage[G] {
      override def snapshot(): G[Map[String, JobStatus]] = funK(storageF.snapshot())

      override def updateJob(key: String, status: JobStatus): G[Unit] = funK(storageF.updateJob(key, status))
    }
}
