package ru.tinkoff.fintech.crwlr.httpserver.storage

import ru.tinkoff.fintech.crwlr.httpserver.JobStatus
import ru.tinkoff.fintech.crwlr.FunK
import slick.dbio.DBIO
import slick.jdbc.H2Profile.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}

class SlickStorage(implicit ec: ExecutionContext) extends JobStorage[DBIO] {
  import SlickStorage._

  override def snapshot(): DBIO[Map[String, JobStatus]] =
    for (statuses <- jobs.result)
      yield statuses.map(s => s.id -> JobStatus(s.isCompleted, Map.empty)).toMap


  override def updateJob(key: String, status: JobStatus): DBIO[Unit] =
    jobs.insertOrUpdate(Job(key, status.isCompleted)).map(_ => ())
}

object SlickStorage {
  def dbioToFuture(db: Database): FunK[DBIO, Future] = new FunK[DBIO, Future] {
    override def apply[A](h: => DBIO[A]): Future[A] = db.run(h)
  }

  def build[F[_]](implicit dbExecutor: FunK[DBIO, F], ec: ExecutionContext): JobStorage[F] =
    JobStorage.convertedStorage(new SlickStorage, dbExecutor)

  def setup: DBIO[Unit] = jobs.schema.create

  class Jobs(tag: Tag) extends Table[Job](tag, "JOBS") {
    def id = column[String]("ID", O.PrimaryKey)
    def isCompleted = column[Boolean]("IS_COMPLETED")

    def * = (id, isCompleted) <> (Job.tupled, Job.unapply)
  }

  val jobs = TableQuery[Jobs]
}
