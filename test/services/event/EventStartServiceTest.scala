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

import models.assessment.Answer
import models.dao._
import models.project.{ActiveProject, Project, Relation}
import models.user.User
import models.{ListWithTotal, NamedEntity}
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.Inspectors.{forAll => testForAll}
import services.{BaseServiceTest, FormService, UserService}
import testutils.fixture.{ActiveProjectFixture, ProjectRelationFixture}
import testutils.generator._

/**
  * Test for event start service.
  */
class EventStartServiceTest
  extends BaseServiceTest
  with ProjectRelationGenerator
  with UserGenerator
  with ProjectGenerator
  with ActiveProjectGenerator
  with AnswerGenerator
  with ListWithTotalGenerator {

  private case class Fixture(
    eventDao: EventDao,
    formService: FormService,
    projectDao: ProjectDao,
    relationDao: ProjectRelationDao,
    activeProjectService: ActiveProjectService,
    userService: UserService,
    answerDao: AnswerDao,
    competenceDao: CompetenceDao,
    competenceGroupDao: CompetenceGroupDao,
    service: EventStartService
  )

  private def getFixture = {
    val eventDao = mock[EventDao]
    val formService = mock[FormService]
    val projectDao = mock[ProjectDao]
    val relationDao = mock[ProjectRelationDao]
    val activeProjectService = mock[ActiveProjectService]
    val userService = mock[UserService]
    val answerDao = mock[AnswerDao]
    val competenceDao = mock[CompetenceDao]
    val competenceGroupDao = mock[CompetenceGroupDao]
    val service = new EventStartService(
      eventDao,
      formService,
      projectDao,
      relationDao,
      activeProjectService,
      userService,
      answerDao,
      competenceDao,
      competenceGroupDao,
      actorSystem
    )
    Fixture(
      eventDao,
      formService,
      projectDao,
      relationDao,
      activeProjectService,
      userService,
      answerDao,
      competenceDao,
      competenceGroupDao,
      service
    )
  }

  "getAnswersForRelation" should {
    "return amount of answers = amount of users for survey" in {
      forAll {
        (
          users: ListWithTotal[User]
        ) =>
          val fixture = getFixture
          val relation = ProjectRelationFixture.surveyRelation
          when(fixture.userService.listByGroupId(eqTo(relation.groupFrom.id), eqTo(false))(*))
            .thenReturn(toSuccessResult(users))

          val result = wait(fixture.service.getAnswersForRelation(relation, ActiveProjectFixture.values(0)).run)

          result mustBe right
          result.toOption.get must have size users.data.size
      }
    }

    "return valid answers for all combinations" in {
      forAll {
        (
          usersFrom: ListWithTotal[User],
          usersTo: ListWithTotal[User]
        ) =>
          val fixture = getFixture
          val groupFromId = 1
          val groupToId = 2
          val relation = ProjectRelationFixture.classicRelation
            .copy(groupFrom = NamedEntity(groupFromId), groupTo = Some(NamedEntity(groupToId)))
          when(fixture.userService.listByGroupId(eqTo(relation.groupFrom.id), eqTo(false))(*))
            .thenReturn(toSuccessResult(usersFrom))
          when(fixture.userService.listByGroupId(eqTo(groupToId), eqTo(false))(*))
            .thenReturn(toSuccessResult(usersTo))

          val result = wait(fixture.service.getAnswersForRelation(relation, ActiveProjectFixture.values(0)).run)

          result mustBe right
          val answers = result.toOption.get
          answers must have size usersFrom.data.size * usersTo.data.size
          testForAll(answers) { answer =>
            answer.activeProjectId mustBe ActiveProjectFixture.values(0).id
            usersFrom.data.map(_.id) must contain(answer.userFromId)
            answer.form mustBe relation.form
            answer.canSkip mustBe relation.canSkipAnswers
          }
      }
    }
  }

  "getAnswerForProject" should {
    "create active project, auditors and get answers for each relation" in {
      forAll {
        (
          eventId: Long,
          project: Project,
          activeProject: ActiveProject,
          auditors: ListWithTotal[User],
          relations: ListWithTotal[Relation]
        ) =>
          val fixture = getFixture
          when(fixture.activeProjectService.create(project, eventId)).thenReturn(toSuccessResult(activeProject))
          when(fixture.userService.listByGroupId(eqTo(project.groupAuditor.id), eqTo(false))(*))
            .thenReturn(toSuccessResult(auditors))
          when(fixture.activeProjectService.createProjectAuditors(activeProject.id, auditors.data.map(_.id)))
            .thenReturn(toSuccessResult(()))
          when(
            fixture.relationDao.getList(
              optId = *,
              optProjectId = eqTo(Some(project.id)),
              optKind = *,
              optFormId = *,
              optGroupFromId = *,
              optGroupToId = *,
              optEmailTemplateId = *
            )(*)
          ).thenReturn(toFuture(relations))

          val answerProducerResult = toSuccessResult(Seq.empty[Answer])

          val result = wait(fixture.service.getAnswersForProject(eventId, project, (_, _) => answerProducerResult).run)

          result mustBe right
          result.toOption.get mustBe empty
      }
    }
  }

  "distinctAnswers" should {
    "not create new answers" in {
      forAll { answers: Seq[Answer] =>
        getFixture.service.distinctAnswers(answers).length must be <= answers.length
      }
    }

    "return single answer" in {
      forAll(answerFormArb.arbitrary, Gen.choose(1, 1000)) { (answer: Answer, times: Int) =>
        val duplicateAnswers = (0 to times).map(_ => answer)
        getFixture.service.distinctAnswers(duplicateAnswers) must have length 1
      }
    }

    "do nothing with empty seq" in {
      getFixture.service.distinctAnswers(Nil) must have length 0
    }
  }
}
