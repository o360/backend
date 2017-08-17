package models.dao

import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * Component for event_project table.
  */
trait EventProjectComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  case class DbEventProject(eventId: Long, projectId: Long)

  class EventProjectTable(tag: Tag) extends Table[DbEventProject](tag, "event_project") {
    def eventId = column[Long]("event_id")
    def projectId = column[Long]("project_id")

    def * = (eventId, projectId) <> ((DbEventProject.apply _).tupled, DbEventProject.unapply)
  }

  val EventProjects = TableQuery[EventProjectTable]
}

/**
  * DAO for event - project relation.
  */
@Singleton
class EventProjectDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with EventProjectComponent
  with DaoHelper {

  import profile.api._

  def exists(eventId: Option[Long], projectId: Option[Long]): Future[Boolean] = db.run {
    EventProjects
      .applyFilter { x =>
        Seq(
          eventId.map(x.eventId === _),
          projectId.map(x.projectId === _)
        )
      }
      .exists
      .result
  }

  def add(eventId: Long, projectId: Long): Future[Unit] =
    db.run {
        EventProjects += DbEventProject(eventId, projectId)
      }
      .map(_ => ())

  def remove(eventId: Long, projectId: Long): Future[Unit] =
    db.run {
        EventProjects.filter(x => x.eventId === eventId && x.projectId === projectId).delete
      }
      .map(_ => ())
}
