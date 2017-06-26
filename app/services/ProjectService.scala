package services

import javax.inject.{Inject, Singleton}

import models.NamedEntity
import models.dao.{EventDao, GroupDao, ProjectDao}
import models.event.Event
import models.project.Project
import models.user.User
import play.api.libs.concurrent.Execution.Implicits._
import utils.errors.{ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.Future

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
  def getList(eventId: Option[Long], groupId: Option[Long], onlyAvailable: Boolean)(implicit account: User, meta: ListMeta): ListResult = {

    val groupFromFilter = if (!onlyAvailable && account.role == User.Role.Admin) None.toFuture
    else groupDao.findGroupIdsByUserId(account.id).map(Some(_))

    for {
      groupFromIds <- groupFromFilter.lift
      projects <- projectDao.getList(
        optId = None,
        optEventId = eventId,
        optGroupFromIds = groupFromIds,
        optAnyRelatedGroupId = groupId
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

    def getConflictedEntities: Future[Option[Map[String, Seq[NamedEntity]]]] = {
      for {
        events <- eventDao.getList(optProjectId = Some(id))
      } yield {
        ConflictError.getConflictedEntitiesMap(
          Event.namePlural -> events.data.map(_.toNamedEntity(account.timezone))
        )
      }
    }

    for {
      _ <- getById(id)

    conflictedEntities <- getConflictedEntities.lift
    _ <- ensure(conflictedEntities.isEmpty) {
      ConflictError.General(Some(Project.nameSingular), conflictedEntities)
    }

     _ <- projectDao.delete(id).lift
    } yield ()
  }
}
