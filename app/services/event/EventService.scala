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

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, Period}

import javax.inject.{Inject, Singleton}
import models.ListWithTotal
import models.assessment.Answer
import models.dao._
import models.event.Event
import models.user.User
import services.ServiceResults
import services.authorization.EventSda
import utils.TimestampConverter
import utils.errors.{BadRequestError, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

/**
  * Event service.
  */
@Singleton
class EventService @Inject() (
  protected val eventDao: EventDao,
  protected val groupDao: GroupDao,
  protected val projectDao: ProjectDao,
  protected val eventProjectDao: EventProjectDao,
  protected val eventJobService: EventJobService,
  protected val answerDao: AnswerDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[Event] {

  /**
    * Returns event by ID
    */
  def getById(id: Long): SingleResult = {
    eventDao
      .findById(id)
      .liftRight {
        NotFoundError.Event(id)
      }
  }

  def getByIdWithAuth(id: Long)(implicit account: User): SingleResult = {
    for {
      event <- getById(id)
      answers <- answerDao.getList(optEventId = Some(event.id), optUserFromId = Some(account.id)).lift
      _ <- ensure(answers.nonEmpty || event.status == Event.Status.NotStarted) { // TODO: proper validation for not started events
        NotFoundError.Event(id)
      }
    } yield event.copy(userInfo = Some(getUserInfo(answers)))
  }

  /**
    * Returns events list filtered by given criteria.
    *
    * @param status        event status
    * @param projectId     event containing project ID
    */
  def list(
    status: Option[Event.Status],
    projectId: Option[Long]
  )(implicit meta: ListMeta): ListResult = {

    eventDao
      .getList(
        optStatus = status,
        optProjectId = projectId
      )
      .lift
  }

  def listWithAuth(status: Option[Event.Status])(implicit account: User): ListResult = {
    val groupFromFilter = groupDao.findGroupIdsByUserId(account.id)

    val result = for {
      groupFromIds <- groupFromFilter
      notStartedEvents <- status match {
        case None | Some(Event.Status.NotStarted) =>
          eventDao.getList(optStatus = Some(Event.Status.NotStarted), optGroupFromIds = Some(groupFromIds))
        case _ => ListWithTotal(Seq.empty[Event]).toFuture
      }
      anotherEvents <- eventDao.getList(optStatus = status, optUserId = Some(account.id))

      withUserInfo <- Future.sequence(anotherEvents.data.map { event =>
        answerDao.getList(optEventId = Some(event.id), optUserFromId = Some(account.id)).map { eventAnswers =>
          event.copy(userInfo = Some(getUserInfo(eventAnswers)))
        }
      })
    } yield ListWithTotal(notStartedEvents.data ++ withUserInfo)

    result.lift
  }

  private def getUserInfo(answers: Seq[Answer]) =
    Event.UserInfo(answers.length, answers.count(_.status != Answer.Status.New))

  /**
    * Creates new event.
    *
    * @param event event model
    */
  def create(event: Event): SingleResult = {
    for {
      _ <- validateEvent(event)

      now = TimestampConverter.now
      _ <- ensure(event.start.isAfter(now) && event.end.isAfter(now) && event.notifications.forall(_.time.isAfter(now))) {
        BadRequestError.Event.WrongDates
      }

      created <- eventDao.create(event).lift
      _ <- eventJobService.createJobs(created).lift
    } yield created
  }

  /**
    * Updates event.
    *
    * @param draft event draft
    */
  def update(draft: Event): SingleResult = {
    for {
      original <- getById(draft.id)
      _ <- validateEvent(draft)
      _ <- EventSda.canUpdate(original, draft).liftLeft

      now = TimestampConverter.now
      _ <- ensure((original.status != Event.Status.NotStarted || draft.start.isAfter(now)) && draft.end.isAfter(now)) {
        BadRequestError.Event.WrongDates
      }

      updated <- eventDao.update(draft).lift
      _ <- eventJobService.createJobs(updated).lift
    } yield updated
  }

  /**
    * Removes event.
    *
    * @param id event ID
    */
  def delete(id: Long): UnitResult = {
    for {
      _ <- getById(id)

      _ <- eventDao.delete(id).lift
    } yield ()
  }

  /**
    * Clones event with projects and notifications.
    */
  def cloneEvent(id: Long): SingleResult = {

    /**
      * Moves all event dates to the future.
      */
    def shiftDates(event: Event): Event = {
      val newStartDate = TimestampConverter.now.plus(2, ChronoUnit.DAYS)
      val difference = Period.between(newStartDate.toLocalDate, event.start.toLocalDate)

      def shift(time: LocalDateTime): LocalDateTime = {
        val newValue = time.plus(difference)
        if (newValue.isBefore(newStartDate)) newStartDate
        else newValue
      }

      val notifications = event.notifications.map(n => n.copy(time = shift(n.time)))
      event.copy(start = newStartDate, end = shift(event.end), notifications = notifications)
    }

    for {
      event <- getById(id)
      withShiftedDates = shiftDates(event)

      createdEvent <- create(withShiftedDates)

      projects <- projectDao.getList(optEventId = Some(event.id)).lift
      projectsIds = projects.data.map(_.id)
      _ <- Future.sequence(projectsIds.map(eventProjectDao.add(createdEvent.id, _))).lift
    } yield createdEvent
  }

  private def validateEvent(event: Event): UnitResult = {
    for {
      _ <- ensure(!event.start.isAfter(event.end)) {
        BadRequestError.Event.StartAfterEnd
      }

      isUniqueNotifications = event.notifications
        .map(x => (x.kind, x.recipient))
        .distinct
        .length == event.notifications.length
      _ <- ensure(isUniqueNotifications) {
        BadRequestError.Event.NotUniqueNotifications
      }
    } yield ()
  }
}
