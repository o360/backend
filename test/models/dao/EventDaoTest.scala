package models.dao

import models.event.Event
import org.scalacheck.Gen
import testutils.fixture.{EventFixture, EventProjectFixture, ProjectRelationFixture}
import testutils.generator.EventGenerator

/**
  * Test for event DAO.
  */
class EventDaoTest
  extends BaseDaoTest
  with EventFixture
  with EventGenerator
  with EventProjectFixture
  with ProjectRelationFixture {

  private val dao = inject[EventDao]

  "get" should {
    "return events by specific criteria" in {
      forAll {
        status: Option[Event.Status] =>
          val events = wait(dao.getList(optStatus = status))
          val expectedEvents = Events.filter(u => status.forall(_ == u.status))
          events.total mustBe expectedEvents.length
          events.data must contain theSameElementsAs expectedEvents
      }
    }

    "return events filtered by project" in {
      forAll { (projectId: Option[Long]) =>
        val events = wait(dao.getList(optProjectId = projectId))
        val expectedEvents = Events
          .filter(e => projectId.forall(p => EventProjects.filter(_._2 == p).map(_._1).contains(e.id)))

        events.data must contain theSameElementsAs expectedEvents
      }
    }

    "return events filtered by groupFrom ids" in {
      val eventsOne = wait(dao.getList(optGroupFromIds = Some(Seq(1))))
      val eventsTwo = wait(dao.getList(optGroupFromIds = Some(Seq(2))))
      val eventsEmpty = wait(dao.getList(optGroupFromIds = Some(Seq(3))))

      val expectedEventsIds = Seq(1, 3)

      eventsEmpty.total mustBe 0
      eventsOne mustBe eventsTwo
      eventsOne.data.map(_.id) must contain theSameElementsAs expectedEventsIds
    }
  }

  "findById" should {
    "return event by ID" in {
      forAll(Gen.choose(0L, Events.length)) { (id: Long) =>
        val event = wait(dao.findById(id))
        val expectedEvent = Events.find(_.id == id)

        event mustBe expectedEvent
      }
    }
  }

  "create" should {
    "create event with relations" in {
      forAll(Gen.oneOf(Events)) { (event: Event) =>
        val created = wait(dao.create(event))

        val eventFromDb = wait(dao.findById(created.id))
        eventFromDb mustBe defined
        created mustBe eventFromDb.get
      }
    }
  }

  "delete" should {
    "delete event with elements" in {
      forAll(Gen.oneOf(Events)) { (event: Event) =>
        val created = wait(dao.create(event))

        val rowsDeleted = wait(dao.delete(created.id))

        val eventFromDb = wait(dao.findById(created.id))
        rowsDeleted mustBe 1
        eventFromDb mustBe empty
      }
    }
  }
  "update" should {
    "update event" in {
      val newEventId = wait(dao.create(Events(0))).id

      forAll(Gen.oneOf(Events)) { (event: Event) =>
        val eventWithId = event.copy(id = newEventId)

        wait(dao.update(eventWithId))

        val updatedFromDb = wait(dao.findById(newEventId))

        updatedFromDb mustBe defined
        updatedFromDb.get mustBe eventWithId
      }
    }
  }
}
