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
    def loadJobs: ConnectionIO[List[Job]] =
      sql"SELECT id, is_completed FROM JOBS".query[Job].to[List]

    def loadHostCounters(jobId: String): ConnectionIO[List[HostCounter]] =
      sql"SELECT job_id, host, counter FROM host_counters WHERE job_id = $jobId".query[HostCounter].to[List]

    for {
      statuses <- loadJobs
      hostCounters <- statuses.traverse(s => loadHostCounters(s.id).map(hcs => s -> hcs))
    } yield hostCounters.map { case (s, hcs) =>
      s.id -> JobStatus(s.isCompleted, hcs.map(h => h.host -> h.counter).toMap)
    }.toMap
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
