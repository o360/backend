package services.event

import javax.inject.Inject

import models.dao.ActiveProjectDao
import models.project.{ActiveProject, Project}
import services.ServiceResults
import models.user.User
import utils.errors.NotFoundError
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

/**
  * Service for active projects.
  */
class ActiveProjectService @Inject()(
  activeProjectDao: ActiveProjectDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[ActiveProject] {

  /**
    * Returns list of active projects.
    */
  def getList(eventId: Option[Long])(implicit account: User, meta: ListMeta): ListResult = {
    activeProjectDao.getList(optEventId = eventId, optUserId = Some(account.id)).lift
  }

  /**
    * Creates active project from project.
    */
  def create(project: Project, eventId: Long): SingleResult = {
    val activeProject = ActiveProject(
      0,
      eventId,
      project.name,
      project.description,
      project.formsOnSamePage,
      project.canRevote,
      project.isAnonymous,
      project.machineName,
      Some(project.id)
    )

    activeProjectDao.create(activeProject).lift
  }

  /**
    * Returns active project by ID.
    */
  def getById(id: Long)(implicit account: User): SingleResult = {
    for {
      projects <- activeProjectDao.getList(optId = Some(id), optUserId = Some(account.id)).lift
      project = projects.data.headOption
      _ <- ensure(project.nonEmpty) {
        NotFoundError.ActiveProject(id)
      }
    } yield project.get
  }

  /**
    * Adds all auditors to active project.
    */
  def createProjectAuditors(apId: Long, auditorsIds: Seq[Long]): UnitResult = {
    Future.sequence(auditorsIds.map(activeProjectDao.addAuditor(apId, _))).map(_ => ()).lift
  }

}
