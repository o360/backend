/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.event

import javax.inject.{Inject, Singleton}

import models.dao.EventProjectDao
import models.event.Event
import services.{ProjectService, ServiceResults}
import utils.errors.AuthorizationError
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext
import scalaz.Scalaz._

/**
  * Project event service.
  */
@Singleton
class EventProjectService @Inject() (
  protected val projectService: ProjectService,
  protected val eventService: EventService,
  protected val eventProjectDao: EventProjectDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[Unit] {

  /**
    * Checks whether project and event available.
    *
    * @param eventId event ID
    * @param projectId  project ID
    * @return none in case of success, some error otherwise
    */
  private def validateEventProject(eventId: Long, projectId: Long): UnitResult = {
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
  def add(eventId: Long, projectId: Long): UnitResult = {
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
  def remove(eventId: Long, projectId: Long): UnitResult = {
    for {
      _ <- validateEventProject(eventId, projectId)
      _ <- eventProjectDao.remove(eventId, projectId).lift
    } yield ()
  }
}
