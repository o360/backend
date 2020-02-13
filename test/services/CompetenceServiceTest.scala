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

import models.competence.Competence
import models.dao._
import models.{EntityKind, ListWithTotal}
import org.mockito.Mockito._
import testutils.fixture.{CompetenceFixture, CompetenceGroupFixture}
import testutils.generator.CompetenceGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

/**
  * Test for competence service.
  */
class CompetenceServiceTest extends BaseServiceTest with CompetenceGenerator {

  private case class TestFixture(
    competenceDao: CompetenceDao,
    competenceGroupService: CompetenceGroupService,
    service: CompetenceService
  )

  private def getFixture = {
    val competenceDao = mock[CompetenceDao]
    val competenceGroupService = mock[CompetenceGroupService]
    val service = new CompetenceService(competenceDao, competenceGroupService, ec)
    TestFixture(competenceDao, competenceGroupService, service)
  }

  "getById" should {

    "return not found if competence not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.competenceDao.getById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return competence from db" in {
      forAll { (competence: Competence, id: Long) =>
        val fixture = getFixture
        when(fixture.competenceDao.getById(id)).thenReturn(toFuture(Some(competence)))
        val result = wait(fixture.service.getById(id).run)

        result mustBe right
        result.toOption.get mustBe competence
      }
    }
  }

  "list" should {
    "return list of competences from db" in {
      forAll {
        (
          groupId: Option[Long],
          competences: Seq[Competence],
          total: Int
        ) =>
          val fixture = getFixture
          when(
            fixture.competenceDao.getList(
              optGroupId = eqTo(groupId),
              optKind = eqTo(Some(EntityKind.Template)),
              optIds = *
            )(eqTo(ListMeta.default))
          ).thenReturn(toFuture(ListWithTotal(total, competences)))
          val result = wait(fixture.service.getList(groupId)(ListMeta.default).run)

          result mustBe right
          result.toOption.get mustBe ListWithTotal(total, competences)
      }
    }
  }

  "create" should {
    "create competence in db" in {
      val competence = CompetenceFixture.values(0)

      val fixture = getFixture
      when(fixture.competenceGroupService.getById(competence.groupId))
        .thenReturn(toSuccessResult(CompetenceGroupFixture.values(0)))
      when(fixture.competenceDao.create(competence.copy(id = 0))).thenReturn(toFuture(competence))
      val result = wait(fixture.service.create(competence.copy(id = 0)).run)

      result mustBe right
      result.toOption.get mustBe competence
    }
  }

  "update" should {
    "return not found if competence not found" in {
      forAll { (competence: Competence) =>
        val fixture = getFixture

        when(fixture.competenceDao.getById(competence.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(competence).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "update competence in db" in {
      val competence = CompetenceFixture.values(0)
      val fixture = getFixture
      when(fixture.competenceDao.getById(competence.id)).thenReturn(toFuture(Some(competence)))
      when(fixture.competenceGroupService.getById(competence.groupId))
        .thenReturn(toSuccessResult(CompetenceGroupFixture.values(0)))
      when(fixture.competenceDao.update(competence)).thenReturn(toFuture(competence))
      val result = wait(fixture.service.update(competence).run)

      result mustBe right
      result.toOption.get mustBe competence
    }
  }

  "delete" should {
    "return not found if competence not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.competenceDao.getById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "delete competence from db" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.competenceDao.getById(id)).thenReturn(toFuture(Some(CompetenceFixture.values(0))))
        when(fixture.competenceDao.delete(id)).thenReturn(toFuture(()))

        val result = wait(fixture.service.delete(id).run)

        result mustBe right
      }
    }
  }
}
