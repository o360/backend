package models.dao

import testutils.fixture.UserGroupFixture

/**
  * Test for user-group DAO.
  */
class UserGroupDaoTest extends BaseDaoTest with UserGroupFixture {

  private val dao = inject[UserGroupDao]

  "exists" should {
    "return true if relation exists" in {
      forAll { (groupId: Option[Long], userId: Option[Long]) =>
        val exists = wait(dao.exists(groupId, userId))

        val expectedExists = UserGroups.exists(x => userId.forall(_ == x._1) && groupId.forall(_ == x._2))

        exists mustBe expectedExists
      }
    }
  }

  "add" should {
    "add user to group" in {
      val groupId = 5
      val userId = 1
      val existsBeforeAdding = wait(dao.exists(groupId = Some(groupId), userId = Some(userId)))
      wait(dao.add(groupId, userId))
      val existsAfterAdding = wait(dao.exists(groupId = Some(groupId), userId = Some(userId)))

      existsBeforeAdding mustBe false
      existsAfterAdding mustBe true
    }
  }

  "remove" should {
    "remove user from group" in {
      val groupId = 3
      val userId = 2
      val existsBeforeRemoving = wait(dao.exists(groupId = Some(groupId), userId = Some(userId)))
      wait(dao.remove(groupId, userId))
      val existsAfterRemoving = wait(dao.exists(groupId = Some(groupId), userId = Some(userId)))

      existsBeforeRemoving mustBe true
      existsAfterRemoving mustBe false
    }
  }
}
