package ru.tinkoff.fintech.crwlr.httpserver.storage

import cats.effect.IO
import doobie._
import doobie.implicits._
import ru.tinkoff.fintech.crwlr.FunK
import ru.tinkoff.fintech.crwlr.httpserver.JobStatus

class DoobieStorage extends JobStorage[ConnectionIO] {

  override def snapshot(): ConnectionIO[Map[String, JobStatus]] =
    for (statuses <- sql"SELECT id, is_completed FROM jobs".query[Job].to[List])
      yield statuses.map(s => s.id -> JobStatus(s.isCompleted, Map.empty)).toMap

  override def updateJob(key: String, status: JobStatus): ConnectionIO[Unit] =
    sql"MERGE INTO jobs(id, is_completed) KEY(id) VALUES ($key, ${status.isCompleted})".update.run.map(_ => ())
}

object DoobieStorage {
  def build[F[_]](implicit dbExecutor: FunK[ConnectionIO, F]): JobStorage[F] =
    JobStorage.convertedStorage(new DoobieStorage, dbExecutor)

  def connectionIOtoIO(tr: Transactor[IO]): FunK[ConnectionIO, IO] = new FunK[ConnectionIO, IO] {
    override def apply[A](h: => doobie.ConnectionIO[A]): IO[A] = h.transact(tr)
  }
}
