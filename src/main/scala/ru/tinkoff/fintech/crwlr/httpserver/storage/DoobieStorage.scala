package ru.tinkoff.fintech.crwlr.httpserver.storage

import cats.effect.IO
import cats.syntax.traverse._
import cats.instances.list._
import doobie._
import doobie.implicits._
import ru.tinkoff.fintech.crwlr.FunK
import ru.tinkoff.fintech.crwlr.httpserver.JobStatus

class DoobieStorage extends JobStorage[ConnectionIO] {

  override def snapshot(): ConnectionIO[Map[String, JobStatus]] = {
    val query = (sql"SELECT id, is_completed, job_id, host, counter FROM jobs, host_counters " ++
      sql"WHERE jobs.id = host_counters.job_id").query[(Job, HostCounter)].to[List]

    for (pairs <- query)
      yield pairs.groupBy(_._1).mapValues(_.map(_._2)).map { case (s, hosts) =>
        s.id -> JobStatus(s.isCompleted, hosts.map(h => h.host -> h.counter).toMap)
      }
  }

  override def updateJob(key: String, status: JobStatus): ConnectionIO[Unit] = {
    def insertOrUpdateJob =
      sql"MERGE INTO jobs(id, is_completed) KEY(id) VALUES ($key, ${status.isCompleted})".update.run

    def insertOrUpdateCounter(host: String, counter: Int) =
      sql"MERGE INTO host_counters(job_id, host, counter) KEY(job_id, host) VALUES ($key, $host, $counter)".update.run

    for {
      _ <- insertOrUpdateJob
      _ <- status.hosts.toList.traverse((insertOrUpdateCounter _).tupled)
    } yield ()
  }
}

object DoobieStorage {
  def build[F[_]](implicit dbExecutor: FunK[ConnectionIO, F]): JobStorage[F] =
    JobStorage.convertedStorage(new DoobieStorage, dbExecutor)

  def connectionIOtoIO(tr: Transactor[IO]): FunK[ConnectionIO, IO] = new FunK[ConnectionIO, IO] {
    override def apply[A](h: => doobie.ConnectionIO[A]): IO[A] = h.transact(tr)
  }
}
