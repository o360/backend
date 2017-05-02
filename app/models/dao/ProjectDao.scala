package models.dao

import javax.inject.{Inject, Singleton}

import models.{ListWithTotal, NamedEntity}
import models.project.Project
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Component for project table.
  */
trait ProjectComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  /**
    * Project db model.
    */
  case class DbProject(
    id: Long,
    name: String,
    description: Option[String],
    groupAuditorId: Long
  ) {

    def toModel(groupAuditorName: String) = Project(
      id,
      name,
      description,
      NamedEntity(groupAuditorId, groupAuditorName)
    )
  }

  object DbProject {
    def fromModel(p: Project) = DbProject(
      p.id,
      p.name,
      p.description,
      p.groupAuditor.id
    )
  }

  class ProjectTable(tag: Tag) extends Table[DbProject](tag, "project") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def groupAuditorId = column[Long]("group_auditor_id")

    def * = (id, name, description, groupAuditorId) <> ((DbProject.apply _).tupled, DbProject.unapply)
  }

  val Projects = TableQuery[ProjectTable]


}

/**
  * Project DAO.
  */
@Singleton
class ProjectDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with ProjectComponent
  with EventProjectComponent
  with GroupComponent
  with DaoHelper {

  import driver.api._

  /**
    * Returns list of projects with relations.
    *
    * @param meta sorting and pagination
    */
  def getList(
    optId: Option[Long] = None,
    optEventId: Option[Long] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Project]] = {
    val query = Projects
      .applyFilter { x =>
        Seq(
          optId.map(x.id === _),
          optEventId.map { eventId =>
            EventProjects.filter(ep => ep.projectId === x.id && ep.eventId === eventId).exists
          }
        )
      }
    .join(Groups).on(_.groupAuditorId === _.id)

    runListQuery(query) {
      case (project, _) => {
        case 'id => project.id
        case 'name => project.name
        case 'description => project.description
      }
    }.map { case ListWithTotal(total, data) =>
      ListWithTotal(total, data.map { case (project, groupAuditor) => project.toModel(groupAuditor.name) })
    }
  }

  /**
    * Returns project by ID
    *
    * @param id project ID
    */
  def findById(id: Long): Future[Option[Project]] = {
    getList(optId = Some(id)).map(_.data.headOption)
  }

  /**
    * Creates project.
    *
    * @param project project model
    * @return created project with ID
    */
  def create(project: Project): Future[Project] = {
    db.run(Projects.returning(Projects.map(_.id))
      += DbProject.fromModel(project))
      .flatMap(findById(_).map(_.get))
  }

  /**
    * Updates project.
    *
    * @param project project model
    * @return updated project
    */
  def update(project: Project): Future[Project] = {
    db.run(Projects.filter(_.id === project.id)
      .update(DbProject.fromModel(project)))
      .flatMap(_ => findById(project.id).map(_.get))
  }

  /**
    * Removes project with relations.
    *
    * @param projectId project ID
    * @return number of rows affected
    */
  def delete(projectId: Long): Future[Int] = db.run {
    Projects.filter(_.id === projectId).delete
  }
}
