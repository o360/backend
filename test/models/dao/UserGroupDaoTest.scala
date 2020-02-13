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
