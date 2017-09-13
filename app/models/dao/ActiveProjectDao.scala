package models.dao

import javax.inject.Inject

import models.ListWithTotal
import models.project.ActiveProject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

/**
  * Active project component.
  */
trait ActiveProjectComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  case class DbActiveProject(
    id: Long,
    eventId: Long,
    name: String,
    description: Option[String],
    formsOnSamePage: Boolean,
    canRevote: Boolean,
    isAnonymous: Boolean,
    machineName: String,
    parentProjectId: Option[Long]
  ) {
    def toModel = ActiveProject(
      id,
      eventId,
      name,
      description,
      formsOnSamePage,
      canRevote,
      isAnonymous,
      machineName,
      parentProjectId
    )
  }

  object DbActiveProject {
    def fromModel(ap: ActiveProject) = DbActiveProject(
      ap.id,
      ap.eventId,
      ap.name,
      ap.description,
      ap.formsOnSamePage,
      ap.canRevote,
      ap.isAnonymous,
      ap.machineName,
      ap.parentProjectId
    )
  }

  class ActiveProjectTable(tag: Tag) extends Table[DbActiveProject](tag, "active_project") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def eventId = column[Long]("event_id")
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def formsOnSamePage = column[Boolean]("forms_on_same_page")
    def canRevote = column[Boolean]("can_revote")
    def isAnonymous = column[Boolean]("is_anonymous")
    def machineName = column[String]("machine_name")
    def parentProjectId = column[Option[Long]]("parent_project_id")

    def * =
      (id, eventId, name, description, formsOnSamePage, canRevote, isAnonymous, machineName, parentProjectId) <> ((DbActiveProject.apply _).tupled, DbActiveProject.unapply)
  }

  val ActiveProjects = TableQuery[ActiveProjectTable]

  case class DbActiveProjectAuditor(
    activeProjectId: Long,
    userId: Long
  )

  class ActiveProjectAuditor(tag: Tag) extends Table[DbActiveProjectAuditor](tag, "active_project_auditor") {

    def activeProjectId = column[Long]("active_project_id")
    def userId = column[Long]("user_id")

    def * = (activeProjectId, userId) <> ((DbActiveProjectAuditor.apply _).tupled, DbActiveProjectAuditor.unapply)
  }

  val ActiveProjectsAuditors = TableQuery[ActiveProjectAuditor]
}

/**
  * Active project DAO.
  */
class ActiveProjectDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with ActiveProjectComponent
  with AnswerComponent
  with DaoHelper {

  import profile.api._

  /**
    * Returns list of active projects.
    */
  def getList(
    optId: Option[Long] = None,
    optUserId: Option[Long] = None,
    optEventId: Option[Long] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[ActiveProject]] = {
    def userFilter(project: ActiveProjectTable) = optUserId.map { userId =>
      project.id.in(Answers.filter(_.userFromId === userId).map(_.activeProjectId))
    }
    val query = ActiveProjects.applyFilter(
      project =>
        Seq(
          optId.map(project.id === _),
          userFilter(project),
          optEventId.map(project.eventId === _)
      ))

    runListQuery(query) { pr =>
      {
        case 'id => pr.id
        case 'name => pr.name
        case 'description => pr.description
      }
    }.map {
      case ListWithTotal(total, data) =>
        ListWithTotal(total, data.map(_.toModel))
    }
  }

  /**
    * Creates active project.
    */
  def create(project: ActiveProject): Future[ActiveProject] = {
    for {
      id <- db.run(ActiveProjects.returning(ActiveProjects.map(_.id)) += DbActiveProject.fromModel(project))
    } yield project.copy(id = id)
  }

  /**
    * Adds auditor to active project.
    */
  def addAuditor(activeProjectId: Long, userId: Long): Future[Unit] = {
    db.run(ActiveProjectsAuditors += DbActiveProjectAuditor(activeProjectId, userId)).map(_ => ())
  }

  /**
    * Returns true if user is auditor of project.
    */
  def isAuditor(activeProjectId: Long, userId: Long): Future[Boolean] = {
    db.run {
      ActiveProjectsAuditors.filter(x => x.userId === userId && x.activeProjectId === activeProjectId).exists.result
    }
  }
}
