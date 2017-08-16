package services

import javax.inject.{Inject, Singleton}

import models.dao.EventProjectDao
import models.event.Event
import models.user.User
import utils.errors.AuthorizationError
import utils.implicits.FutureLifting._

import scalaz.Scalaz._

/**
  * Project event service.
  */
@Singleton
class EventProjectService @Inject()(
  protected val projectService: ProjectService,
  protected val eventService: EventService,
  protected val eventProjectDao: EventProjectDao
) extends ServiceResults[Unit] {

  /**
    * Checks whether project and event available.
    *
    * @param eventId event ID
    * @param projectId  project ID
    * @return none in case of success, some error otherwise
    */
  private def validateEventProject(eventId: Long, projectId: Long)(implicit account: User): UnitResult = {
    for {
      _ <- projectService.getById(projectId)
      event <- eventService.getById(eventId)

      _ <- ensure(event.status != Event.Status.InProgress) {
        AuthorizationError.ProjectsInEventUpdating
      }
    } yield ()
  }

  /**
    * Adds project to event.
    *
    * @param eventId event ID
    * @param projectId  project ID
    */
  def add(eventId: Long, projectId: Long)(implicit account: User): UnitResult = {
    for {
      _ <- validateEventProject(eventId, projectId)

      isAlreadyExists <- eventProjectDao.exists(Some(eventId), Some(projectId)).lift
      _ <- { isAlreadyExists ? ().toFuture | eventProjectDao.add(eventId, projectId) }.lift
    } yield ()
  }

  /**
    * Removes project from event.
    *
    * @param eventId event ID
    * @param projectId  project ID
    */
  def remove(eventId: Long, projectId: Long)(implicit account: User): UnitResult = {
    for {
      _ <- validateEventProject(eventId, projectId)
      _ <- eventProjectDao.remove(eventId, projectId).lift
    } yield ()
  }
}
