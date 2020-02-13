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

import models.competence.{Competence, CompetenceGroup}
import models.dao._
import models.{EntityKind, ListWithTotal}
import org.mockito.Mockito._
import testutils.fixture.{CompetenceFixture, CompetenceGroupFixture}
import testutils.generator.CompetenceGroupGenerator
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

/**
  * Test for competence group service.
  */
class CompetenceGroupServiceTest extends BaseServiceTest with CompetenceGroupGenerator {

  private case class TestFixture(
    competenceDao: CompetenceDao,
    competenceGroupDao: CompetenceGroupDao,
    service: CompetenceGroupService
  )

  private def getFixture = {
    val competenceDao = mock[CompetenceDao]
    val competenceGroupDao = mock[CompetenceGroupDao]
    val service = new CompetenceGroupService(competenceGroupDao, competenceDao, ec)
    TestFixture(competenceDao, competenceGroupDao, service)
  }

  "getById" should {

    "return not found if group not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.competenceGroupDao.getById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return group from db" in {
      forAll { (group: CompetenceGroup, id: Long) =>
        val fixture = getFixture
        when(fixture.competenceGroupDao.getById(id)).thenReturn(toFuture(Some(group)))
        val result = wait(fixture.service.getById(id).run)

        result mustBe right
        result.toOption.get mustBe group
      }
    }
  }

  "list" should {
    "return list of groups from db" in {
      forAll {
        (
          groups: Seq[CompetenceGroup],
          total: Int
        ) =>
          val fixture = getFixture
          when(
            fixture.competenceGroupDao.getList(
              optKind = eqTo(Some(EntityKind.Template)),
              optIds = *
            )(eqTo(ListMeta.default))
          ).thenReturn(toFuture(ListWithTotal(total, groups)))
          val result = wait(fixture.service.getList(ListMeta.default).run)

          result mustBe right
          result.toOption.get mustBe ListWithTotal(total, groups)
      }
    }
  }

  "create" should {
    "create group in db" in {
      val group = CompetenceGroupFixture.values(0)

      val fixture = getFixture
      when(fixture.competenceGroupDao.create(group.copy(id = 0))).thenReturn(toFuture(group))
      val result = wait(fixture.service.create(group.copy(id = 0)).run)

      result mustBe right
      result.toOption.get mustBe group
    }
  }

  "update" should {
    "return not found if group not found" in {
      forAll { (group: CompetenceGroup) =>
        val fixture = getFixture
        when(fixture.competenceGroupDao.getById(group.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(group).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "update group in db" in {
      val group = CompetenceGroupFixture.values(0)
      val fixture = getFixture
      when(fixture.competenceGroupDao.getById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.competenceGroupDao.update(group)).thenReturn(toFuture(group))
      val result = wait(fixture.service.update(group).run)

      result mustBe right
      result.toOption.get mustBe group
    }
  }

  "delete" should {
    "return not found if group not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.competenceGroupDao.getById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return conflict error if can't delete" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.competenceGroupDao.getById(id)).thenReturn(toFuture(Some(CompetenceGroupFixture.values(0))))
        when(
          fixture.competenceDao.getList(
            optGroupId = eqTo(Some(id)),
            optKind = *,
            optIds = *
          )(*)
        ).thenReturn(toFuture(ListWithTotal(CompetenceFixture.values.take(1))))

        val result = wait(fixture.service.delete(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "delete group from db" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.competenceGroupDao.getById(id)).thenReturn(toFuture(Some(CompetenceGroupFixture.values(0))))
        when(
          fixture.competenceDao.getList(
            optGroupId = eqTo(Some(id)),
            optKind = *,
            optIds = *
          )(*)
        ).thenReturn(toFuture(ListWithTotal(Seq.empty[Competence])))

        when(fixture.competenceGroupDao.delete(id)).thenReturn(toFuture(()))

        val result = wait(fixture.service.delete(id).run)

        result mustBe right
      }
    }
  }
}
