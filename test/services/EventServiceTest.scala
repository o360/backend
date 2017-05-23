package services

import java.sql.{SQLException, Timestamp}

import models.ListWithTotal
import models.dao.{EventDao, GroupDao}
import models.event.Event
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{EventFixture, UserFixture}
import testutils.generator.EventGenerator
import utils.errors.{BadRequestError, ConflictError, NotFoundError}
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Test for event service.
  */
class EventServiceTest extends BaseServiceTest with EventGenerator with EventFixture {

  private val admin = UserFixture.admin

  private case class TestFixture(
    eventDaoMock: EventDao,
    groupDao: GroupDao,
    service: EventService)

  private def getFixture = {
    val daoMock = mock[EventDao]
    val groupDao = mock[GroupDao]
    val service = new EventService(daoMock, groupDao)
    TestFixture(daoMock, groupDao, service)
  }

  "getById" should {

    "return not found if event not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.eventDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.eventDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.eventDaoMock)
      }
    }

    "return event from db" in {
      forAll { (event: Event, id: Long) =>
        val fixture = getFixture
        when(fixture.eventDaoMock.findById(id)).thenReturn(toFuture(Some(event)))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'right
        result.toOption.get mustBe event

        verify(fixture.eventDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.eventDaoMock)
      }
    }
  }

  "list" should {
    "return list of events from db for admin" in {
      forAll { (
      projectId: Option[Long],
      status: Option[Event.Status],
      events: Seq[Event],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.eventDaoMock.getList(
          optId = any[Option[Long]],
          optStatus = eqTo(status),
          optProjectId = eqTo(projectId),
          optNotificationFrom = any[Option[Timestamp]],
          optNotificationTo = any[Option[Timestamp]],
          optFormId = any[Option[Long]],
          optGroupFromIds = eqTo(None),
          optEndFrom = any[Option[Timestamp]],
          optEndTimeTo = any[Option[Timestamp]]
        )(eqTo(ListMeta.default)))
          .thenReturn(toFuture(ListWithTotal(total, events)))
        val result = wait(fixture.service.list(status, projectId)(admin, ListMeta.default).run)

        result mustBe 'right
        result.toOption.get mustBe ListWithTotal(total, events)
      }
    }

    "filter events by groupFrom for user" in {
      val user = UserFixture.user
      forAll { (
      projectId: Option[Long],
      status: Option[Event.Status],
      userGroups: Seq[Long],
      events: Seq[Event],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.eventDaoMock.getList(
          optId = any[Option[Long]],
          optStatus = eqTo(status),
          optProjectId = eqTo(projectId),
          optNotificationFrom = any[Option[Timestamp]],
          optNotificationTo = any[Option[Timestamp]],
          optFormId = any[Option[Long]],
          optGroupFromIds = eqTo(Some(userGroups)),
          optEndFrom = any[Option[Timestamp]],
          optEndTimeTo = any[Option[Timestamp]]
        )(eqTo(ListMeta.default)))
          .thenReturn(toFuture(ListWithTotal(total, events)))
        when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroups))
        val result = wait(fixture.service.list(status, projectId)(user, ListMeta.default).run)

        result mustBe 'right
        result.toOption.get mustBe ListWithTotal(total, events)
      }
    }
  }

  "create" should {
    "return conflict if can't validate event" in {
      forAll { (event: Event) =>
        whenever((event.start after event.end) ||
          event.notifications.map(x => (x.kind, x.recipient)).distinct.length != event.notifications.length) {
          val fixture = getFixture

          val result = wait(fixture.service.create(event)(admin).run)

          result mustBe 'left
          result.swap.toOption.get mustBe a[BadRequestError]
        }
      }
    }

    "create event in db" in {
      val event = Events(0)

      val fixture = getFixture
      when(fixture.eventDaoMock.create(event.copy(id = 0))).thenReturn(toFuture(event))
      val result = wait(fixture.service.create(event.copy(id = 0))(admin).run)

      result mustBe 'right
      result.toOption.get mustBe event
    }
  }

  "update" should {
    "return conflict if can't validate event" in {
      forAll { (event: Event) =>
        whenever((event.start after event.end) ||
          event.notifications.map(x => (x.kind, x.recipient)).distinct.length != event.notifications.length) {          val fixture = getFixture
          when(fixture.eventDaoMock.findById(event.id)).thenReturn(toFuture(Some(event)))
          when(fixture.eventDaoMock.update(any[Event])).thenReturn(Future.failed(new SQLException("", "2300")))
          val result = wait(fixture.service.update(event.copy(id = 0))(admin).run)

          result mustBe 'left
          result.swap.toOption.get mustBe a[BadRequestError]
        }
      }
    }

    "return not found if event not found" in {
      forAll { (event: Event) =>
        val fixture = getFixture
        when(fixture.eventDaoMock.findById(event.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(event)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.eventDaoMock, times(1)).findById(event.id)
        verifyNoMoreInteractions(fixture.eventDaoMock)
      }
    }

    "update event in db" in {
      val event = Events(0)
      val fixture = getFixture
      when(fixture.eventDaoMock.findById(event.id)).thenReturn(toFuture(Some(event)))
      when(fixture.eventDaoMock.update(event)).thenReturn(toFuture(event))
      val result = wait(fixture.service.update(event)(admin).run)

      result mustBe 'right
      result.toOption.get mustBe event
    }
  }

  "delete" should {
    "return not found if event not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.eventDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.eventDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.eventDaoMock)
      }
    }

    "delete event from db" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.eventDaoMock.findById(id)).thenReturn(toFuture(Some(Events(0))))
        when(fixture.eventDaoMock.delete(id)).thenReturn(toFuture(1))

        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'right
      }
    }
  }
}
