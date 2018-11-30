package ru.tinkoff.fintech.crwlr.httpserver.storage

import ru.tinkoff.fintech.crwlr.httpserver.JobStatus
import ru.tinkoff.fintech.crwlr.FunK
import slick.dbio.DBIO
import slick.jdbc.H2Profile.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}

class SlickStorage(implicit ec: ExecutionContext) extends JobStorage[DBIO] {
  import SlickStorage._

  override def snapshot(): DBIO[Map[String, JobStatus]] = {
    val query = for {
      hostCounter <- hostCounters
      job <- hostCounter.job
    } yield (job, hostCounter)

    for (pairs <- query.result)
      yield pairs.groupBy(_._1).mapValues(_.map(_._2)).map { case (s, hosts) =>
        s.id -> JobStatus(s.isCompleted, hosts.map(h => h.host -> h.counter).toMap)
      }
  }

  override def updateJob(key: String, status: JobStatus): DBIO[Unit] = {
    def insertOrUpdateHostCounter(host: String, counter: Int): DBIO[Unit] =
      hostCounters.insertOrUpdate(HostCounter(key, host, counter)).map(_ => ())

    for {
      _ <- jobs.insertOrUpdate(Job(key, status.isCompleted))
      _ <- DBIO.sequence(status.hosts.map((insertOrUpdateHostCounter _).tupled))
    } yield ()
  }
}

object SlickStorage {
  def dbioToFuture(db: Database): FunK[DBIO, Future] = new FunK[DBIO, Future] {
    override def apply[A](h: => DBIO[A]): Future[A] = db.run(h)
  }

  def build[F[_]](implicit dbExecutor: FunK[DBIO, F], ec: ExecutionContext): JobStorage[F] =
    JobStorage.convertedStorage(new SlickStorage, dbExecutor)

  def setup: DBIO[Unit] = (jobs.schema ++ hostCounters.schema).create

  class Jobs(tag: Tag) extends Table[Job](tag, "JOBS") {
    def id = column[String]("ID", O.PrimaryKey)
    def isCompleted = column[Boolean]("IS_COMPLETED")

    def * = (id, isCompleted) <> (Job.tupled, Job.unapply)
  }

  class HostCounters(tag: Tag) extends Table[HostCounter](tag, "HOST_COUNTERS") {
    def jobId = column[String]("JOB_ID")
    def host = column[String]("HOST")
    def counter = column[Int]("COUNTER")

    def * = (jobId, host, counter) <> (HostCounter.tupled, HostCounter.unapply)

    def pk = primaryKey("PK", (jobId, host))

    def job = foreignKey("JOB_FK", jobId, jobs)(_.id)
  }

  val jobs = TableQuery[Jobs]
  val hostCounters = TableQuery[HostCounters]
}
