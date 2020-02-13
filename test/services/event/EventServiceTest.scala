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

package services

import java.sql.SQLException

import models.ListWithTotal
import models.assessment.Answer
import models.dao._
import models.event.Event
import models.project.Project
import models.user.User
import org.mockito.Mockito._
import services.event.{EventJobService, EventService}
import testutils.fixture.EventFixture
import testutils.generator.{AnswerGenerator, EventGenerator, ListMetaGenerator, UserGenerator}
import utils.errors.{BadRequestError, NotFoundError}
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Test for event service.
  */
class EventServiceTest
  extends BaseServiceTest
  with EventGenerator
  with EventFixture
  with UserGenerator
  with AnswerGenerator
  with ListMetaGenerator {

  private case class TestFixture(
    eventDao: EventDao,
    groupDao: GroupDao,
    projectDao: ProjectDao,
    eventProjectDao: EventProjectDao,
    eventJobService: EventJobService,
    answerDao: AnswerDao,
    service: EventService
  )

  private def getFixture = {
    val daoMock = mock[EventDao]
    val groupDao = mock[GroupDao]
    val projectDao = mock[ProjectDao]
    val eventProjectDao = mock[EventProjectDao]
    val eventJobService = mock[EventJobService]
    val answerDao = mock[AnswerDao]
    val service = new EventService(daoMock, groupDao, projectDao, eventProjectDao, eventJobService, answerDao, ec)
    TestFixture(daoMock, groupDao, projectDao, eventProjectDao, eventJobService, answerDao, service)
  }

  "getById" should {

    "return not found if event not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.eventDao.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return event from db" in {
      forAll { (event: Event, id: Long) =>
        val fixture = getFixture
        when(fixture.eventDao.findById(id)).thenReturn(toFuture(Some(event)))
        val result = wait(fixture.service.getById(id).run)

        result mustBe right
        result.toOption.get mustBe event
      }
    }
  }

  "getByIdWithAuth" should {
    "return not found if event is not available" in {
      forAll { (event: Event, account: User) =>
        whenever(event.status != Event.Status.NotStarted) {
          val fixture = getFixture
          when(fixture.eventDao.findById(event.id)).thenReturn(toFuture(Some(event)))
          when(
            fixture.answerDao.getList(
              optEventId = eqTo(Some(event.id)),
              optActiveProjectId = *,
              optUserFromId = eqTo(Some(account.id)),
              optFormId = *,
              optUserToId = *
            )
          ).thenReturn(toFuture(Nil))

          val result = wait(fixture.service.getByIdWithAuth(event.id)(account).run)

          result mustBe left
          result.swap.toOption.get mustBe a[NotFoundError]
        }
      }
    }

    "return event with user info" in {
      forAll { (event: Event, account: User, answers: Seq[Answer]) =>
        whenever(answers.nonEmpty || event.status == Event.Status.NotStarted) {
          val fixture = getFixture
          when(fixture.eventDao.findById(event.id)).thenReturn(toFuture(Some(event)))
          when(
            fixture.answerDao.getList(
              optEventId = eqTo(Some(event.id)),
              optActiveProjectId = *,
              optUserFromId = eqTo(Some(account.id)),
              optFormId = *,
              optUserToId = *
            )
          ).thenReturn(toFuture(answers))
          val result = wait(fixture.service.getByIdWithAuth(event.id)(account).run)

          val expectedUserInfo = Event.UserInfo(answers.length, answers.count(_.status != Answer.Status.New))
          val expectedEvent = event.copy(userInfo = Some(expectedUserInfo))

          result mustBe right
          result.toOption.get mustBe expectedEvent
        }
      }
    }
  }

  "list" should {
    "return list of events from db" in {
      forAll {
        (
          projectId: Option[Long],
          status: Option[Event.Status],
          events: Seq[Event]
        ) =>
          val fixture = getFixture
          when(
            fixture.eventDao.getList(
              optId = *,
              optStatus = eqTo(status),
              optProjectId = eqTo(projectId),
              optFormId = *,
              optGroupFromIds = *,
              optUserId = *
            )(*)
          ).thenReturn(toFuture(ListWithTotal(events)))
          val result = wait(fixture.service.list(status, projectId)(ListMeta.default).run)

          result mustBe right
          result.toOption.get mustBe ListWithTotal(events)
      }
    }
  }

  "listWithAuth" should {
    "query events by groupsIds for not started filter" in {
      forAll {
        (
          status: Option[Event.Status],
          account: User,
          groupsIds: Seq[Long],
          notStartedEvents: Seq[Event]
        ) =>
          whenever(status.forall(_ == Event.Status.NotStarted)) {
            val fixture = getFixture

            when(fixture.groupDao.findGroupIdsByUserId(account.id)).thenReturn(toFuture(groupsIds))
            when(
              fixture.eventDao.getList(
                optId = *,
                optStatus = eqTo(Some(Event.Status.NotStarted)),
                optProjectId = *,
                optFormId = *,
                optGroupFromIds = eqTo(Some(groupsIds)),
                optUserId = *
              )(*)
            ).thenReturn(toFuture(ListWithTotal(notStartedEvents)))
            when(
              fixture.eventDao.getList(
                optId = *,
                optStatus = eqTo(status),
                optProjectId = *,
                optFormId = *,
                optGroupFromIds = *,
                optUserId = eqTo(Some(account.id))
              )(*)
            ).thenReturn(toFuture(ListWithTotal(Seq.empty[Event])))

            val result = wait(fixture.service.listWithAuth(status)(account).run)

            result mustBe right
            result.toOption.get.data must contain theSameElementsAs notStartedEvents
          }
      }
    }

    "query events by active project for started/completed events" in {
      forAll {
        (
          status: Option[Event.Status],
          account: User,
          events: Seq[Event]
        ) =>
          whenever(!status.forall(_ == Event.Status.NotStarted)) {
            val fixture = getFixture

            when(fixture.groupDao.findGroupIdsByUserId(account.id)).thenReturn(toFuture(Nil))

            when(
              fixture.eventDao.getList(
                optId = *,
                optStatus = eqTo(status),
                optProjectId = *,
                optFormId = *,
                optGroupFromIds = *,
                optUserId = eqTo(Some(account.id))
              )(*)
            ).thenReturn(toFuture(ListWithTotal(events)))

            when(
              fixture.answerDao.getList(
                optEventId = *,
                optActiveProjectId = *,
                optUserFromId = eqTo(Some(account.id)),
                optFormId = *,
                optUserToId = *
              )
            ).thenReturn(toFuture(Nil))

            val result = wait(fixture.service.listWithAuth(status)(account).run)

            val expectedEvents = events.map(_.copy(userInfo = Some(Event.UserInfo(0, 0))))

            result mustBe right
            result.toOption.get.data must contain theSameElementsAs expectedEvents
          }
      }
    }
  }

  "create" should {
    "return conflict if can't validate event" in {
      forAll { (event: Event) =>
        whenever(
          (event.start isAfter event.end) ||
            event.notifications.map(x => (x.kind, x.recipient)).distinct.length != event.notifications.length
        ) {
          val fixture = getFixture

          val result = wait(fixture.service.create(event).run)

          result mustBe left
          result.swap.toOption.get mustBe a[BadRequestError]
        }
      }
    }

    "create event in db" in {
      val event = Events(2)

      val fixture = getFixture
      when(fixture.eventDao.create(event.copy(id = 0))).thenReturn(toFuture(event))
      when(fixture.eventJobService.createJobs(event)).thenReturn(toFuture(()))
      val result = wait(fixture.service.create(event.copy(id = 0)).run)

      result mustBe right
      result.toOption.get mustBe event
    }
  }

  "update" should {
    "return conflict if can't validate event" in {
      forAll { (event: Event) =>
        whenever(
          (event.start isAfter event.end) ||
            event.notifications.map(x => (x.kind, x.recipient)).distinct.length != event.notifications.length
        ) {
          val fixture = getFixture
          when(fixture.eventDao.findById(event.id)).thenReturn(toFuture(Some(event)))
          when(fixture.eventDao.update(*)).thenReturn(Future.failed(new SQLException("", "2300")))
          val result = wait(fixture.service.update(event).run)

          result mustBe left
          result.swap.toOption.get mustBe a[BadRequestError]
        }
      }
    }

    "return not found if event not found" in {
      forAll { (event: Event) =>
        val fixture = getFixture
        when(fixture.eventDao.findById(event.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(event).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.eventDao, times(1)).findById(event.id)
        verifyNoMoreInteractions(fixture.eventDao)
      }
    }

    "update event in db" in {
      val event = Events(2)
      val fixture = getFixture
      when(fixture.eventDao.findById(event.id)).thenReturn(toFuture(Some(event)))
      when(fixture.eventDao.update(event)).thenReturn(toFuture(event))
      when(fixture.eventJobService.createJobs(event)).thenReturn(toFuture(()))
      val result = wait(fixture.service.update(event).run)

      result mustBe right
      result.toOption.get mustBe event
    }
  }

  "delete" should {
    "return not found if event not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.eventDao.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.eventDao, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.eventDao)
      }
    }

    "delete event from db" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.eventDao.findById(id)).thenReturn(toFuture(Some(Events(0))))
        when(fixture.eventDao.delete(id)).thenReturn(toFuture(1))

        val result = wait(fixture.service.delete(id).run)

        result mustBe right
      }
    }
  }

  "cloneEvent" should {
    "return not found if event not found" in {
      forAll { (eventId: Long) =>
        val fixture = getFixture
        when(fixture.eventDao.findById(eventId)).thenReturn(toFuture(None))
        val result = wait(fixture.service.cloneEvent(eventId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "clone event" in {
      val event = Events(1)
      val createdEvent = event.copy(id = 2)

      val fixture = getFixture
      when(fixture.eventDao.findById(event.id)).thenReturn(toFuture(Some(event)))
      when(fixture.eventDao.create(*)).thenReturn(toFuture(createdEvent))
      when(
        fixture.projectDao.getList(
          optId = *,
          optEventId = eqTo(Some(createdEvent.id)),
          optGroupFromIds = *,
          optFormId = *,
          optGroupAuditorId = *,
          optEmailTemplateId = *,
          optAnyRelatedGroupId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal[Project](0, Nil)))
      when(fixture.eventJobService.createJobs(createdEvent)).thenReturn(toFuture(()))

      val result = wait(fixture.service.cloneEvent(event.id).run)

      result mustBe right
      result.toOption.get mustBe createdEvent
    }
  }
}
