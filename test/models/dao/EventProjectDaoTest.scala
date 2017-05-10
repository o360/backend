package models.dao

import testutils.fixture.EventProjectFixture

/**
  * Test for event-project DAO.
  */
class EventProjectDaoTest extends BaseDaoTest with EventProjectFixture {

  private val dao = inject[EventProjectDao]

  "exists" should {
    "return true if relation exists" in {
      forAll { (eventId: Option[Long], projectId: Option[Long]) =>
        val exists = wait(dao.exists(eventId, projectId))

        val expectedExists = EventProjects.exists(x => eventId.forall(_ == x._1) && projectId.forall(_ == x._2))

        exists mustBe expectedExists
      }
    }
  }

  "add" should {
    "add project to event" in {
      val eventId = 4
      val projectId = 1
      val existsBeforeAdding = wait(dao.exists(eventId = Some(eventId), projectId = Some(projectId)))
      wait(dao.add(eventId, projectId))
      val existsAfterAdding = wait(dao.exists(eventId = Some(eventId), projectId = Some(projectId)))

      existsBeforeAdding mustBe false
      existsAfterAdding mustBe true
    }
  }

  "remove" should {
    "remove project from event" in {
      val eventId = 3
      val projectId = 1
      val existsBeforeRemoving = wait(dao.exists(eventId = Some(eventId), projectId = Some(projectId)))
      wait(dao.remove(eventId, projectId))
      val existsAfterRemoving = wait(dao.exists(eventId = Some(eventId), projectId = Some(projectId)))

      existsBeforeRemoving mustBe true
      existsAfterRemoving mustBe false
    }
  }
}
