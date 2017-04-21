package models.dao

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.project.Project
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Component for project and relation tables.
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
    description: Option[String]
  ) {

    def toModel(relations: Seq[Project.Relation]) = Project(
      id,
      name,
      description,
      relations
    )
  }

  class ProjectTable(tag: Tag) extends Table[DbProject](tag, "project") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def description = column[Option[String]]("description")

    def * = (id, name, description) <> ((DbProject.apply _).tupled, DbProject.unapply)
  }

  val Projects = TableQuery[ProjectTable]

  /**
    * Relation db model.
    */
  case class DbRelation(
    projectId: Long,
    groupFromId: Long,
    groupToId: Long,
    groupAuditorId: Long,
    formId: Long
  ) {

    def toModel = Project.Relation(
      groupFromId,
      groupToId,
      groupAuditorId,
      formId
    )
  }

  object DbRelation {
    def fromModel(r: Project.Relation, projectId: Long) = DbRelation(
      projectId,
      r.groupFrom,
      r.groupTo,
      r.groupAuditor,
      r.form
    )
  }

  class RelationTable(tag: Tag) extends Table[DbRelation](tag, "relation") {

    def projectId = column[Long]("project_id")
    def groupFromId = column[Long]("group_from_id")
    def groupToId = column[Long]("group_to_id")
    def groupAuditorId = column[Long]("group_auditor_id")
    def formId = column[Long]("form_id")


    def * = (projectId, groupFromId, groupToId, groupAuditorId, formId) <>
      ((DbRelation.apply _).tupled, DbRelation.unapply)
  }

  val Relations = TableQuery[RelationTable]
}

/**
  * Project DAO.
  */
@Singleton
class ProjectDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with ProjectComponent
  with DaoHelper {

  import driver.api._

  /**
    * Returns list of projects with relations.
    *
    * @param meta sorting and pagination
    */
  def getList(optId: Option[Long] = None)
    (implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Project]] = {
    val baseQuery = Projects.applyFilter(x => Seq(optId.map(x.id === _)))

    val countQuery = baseQuery.length
    val resultQuery = baseQuery
      .applySorting(meta.sorting) {
        project => {
          case 'id => project.id
          case 'name => project.name
          case 'description => project.description
        }
      }
      .applyPagination(meta.pagination)
      .joinLeft(Relations).on(_.id === _.projectId)


    for {
      count <- db.run(countQuery.result)
      flatResult <- if (count > 0) db.run(resultQuery.result) else Nil.toFuture
    } yield {
      val data = flatResult
        .groupByWithOrder { case (project, _) => project }
        .map { case (project, relationsWithProject) =>
          val relations = relationsWithProject
            .collect {
              case (_, Some(relation)) => relation.toModel
            }

          project.toModel(relations)
        }

      ListWithTotal(count, data)
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
    db.run {
      (for {
        projectId <- Projects.returning(Projects.map(_.id)) += DbProject(0, project.name, project.description)
        _ <- DBIO.seq(Relations ++= project.relations.map(DbRelation.fromModel(_, projectId)))
      } yield projectId).transactionally
    }.map(id => project.copy(id = id))
  }

  /**
    * Updates project.
    *
    * @param project project model
    * @return updated project
    */
  def update(project: Project): Future[Project] = {
    db.run {
      (for {
        _ <- Projects.filter(_.id === project.id).update(DbProject(project.id, project.name, project.description))
        _ <- Relations.filter(_.projectId === project.id).delete
        _ <- DBIO.seq(Relations ++= project.relations.map(DbRelation.fromModel(_, project.id)))
      } yield ()).transactionally
    }.map(_ => project)
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
