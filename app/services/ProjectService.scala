package services

import javax.inject.{Inject, Singleton}

import models.dao.{EventDao, ProjectDao}
import models.event.Event
import models.project.Project
import models.user.User
import utils.errors.{ApplicationError, ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scalaz.Scalaz._
import scalaz._

/**
  * Project service.
  */
@Singleton
class ProjectService @Inject()(
  protected val projectDao: ProjectDao,
  protected val eventDao: EventDao
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
  def getList(eventId: Option[Long])(implicit account: User, meta: ListMeta): ListResult =
    projectDao.getList(optId = None, optEventId = eventId).lift

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
