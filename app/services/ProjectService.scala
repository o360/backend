package services

import javax.inject.{Inject, Singleton}

import models.dao.{EventDao, GroupDao, ProjectDao}
import models.event.Event
import models.project.Project
import models.user.User
import utils.errors.{ApplicationError, ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import play.api.libs.concurrent.Execution.Implicits._

import scalaz.Scalaz._
import scalaz._

/**
  * Project service.
  */
@Singleton
class ProjectService @Inject()(
  protected val projectDao: ProjectDao,
  protected val eventDao: EventDao,
  protected val groupDao: GroupDao
) extends ServiceResults[Project] {

  /**
    * Returns project by ID
    */
  def getById(id: Long)(implicit account: User): SingleResult = {
    projectDao.findById(id)
      .liftRight {
        NotFoundError.Project(id)
      }
  }

  /**
    * Returns projects list.
    */
  def getList(eventId: Option[Long])(implicit account: User, meta: ListMeta): ListResult = {

    val groupFromFilter = account.role match {
      case User.Role.Admin => None.toFuture
      case User.Role.User => groupDao.findGroupIdsByUserId(account.id).map(Some(_))
    }

    for {
      groupFromIds <- groupFromFilter.lift
      projects <- projectDao.getList(
        optId = None,
        optEventId = eventId,
        optGroupFromIds = groupFromIds
      ).lift
    } yield projects
  }

  /**
    * Creates new project.
    *
    * @param project project model
    */
  def create(project: Project)(implicit account: User): SingleResult = {
    projectDao.create(project).lift(ExceptionHandler.sql)
  }

  /**
    * Updates project.
    *
    * @param draft project draft
    */
  def update(draft: Project)(implicit account: User): SingleResult = {
    for {
      _ <- getById(draft.id)

      activeEvents <- eventDao.getList(optStatus = Some(Event.Status.InProgress), optProjectId = Some(draft.id)).lift
      _ <- ensure(activeEvents.total == 0) {
        ConflictError.Project.ActiveEventExists
      }

      updated <- projectDao.update(draft).lift(ExceptionHandler.sql)
    } yield updated
  }

  /**
    * Removes project.
    *
    * @param id project ID
    */
  def delete(id: Long)(implicit account: User): UnitResult = {
    for {
      _ <- getById(id)
     _ <- projectDao.delete(id).lift(ExceptionHandler.sql)
    } yield ()
  }
}
