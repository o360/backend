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

package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import controllers.api.project.{ApiPartialRelation, ApiPartialTemplateBinding, ApiRelation}
import models.ListWithTotal
import models.project.Relation
import models.user.User
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ProjectRelationService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.ProjectRelationGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext

/**
  * Test for project relation controller.
  */
class ProjectRelationControllerTest extends BaseControllerTest with ProjectRelationGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    projectServiceMock: ProjectRelationService,
    controller: ProjectRelationController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val projectRelationServiceMock = mock[ProjectRelationService]
    val controller =
      new ProjectRelationController(silhouette, projectRelationServiceMock, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, projectRelationServiceMock, controller)
  }

  private val admin = UserFixture.admin

  "GET /relations/id" should {
    "return not found if relation not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.getById(id))
          .thenReturn(toErrorResult[Relation](NotFoundError.ProjectRelation(id)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with relation" in {
      forAll { (id: Long, relation: Relation) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.getById(id)).thenReturn(toSuccessResult(relation))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val projectJson = contentAsJson(response)
        projectJson mustBe Json.toJson(ApiRelation(relation))
      }
    }
  }

  "GET /relations" should {
    "return relations list from service" in {
      forAll {
        (
          projectId: Option[Long],
          total: Int,
          relations: Seq[Relation]
        ) =>
          val env = fakeEnvironment(admin)
          val fixture = getFixture(env)
          when(fixture.projectServiceMock.getList(projectId)(ListMeta.default))
            .thenReturn(toSuccessResult(ListWithTotal(total, relations)))
          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller.getList(projectId)(request)

          status(response) mustBe OK
          val relationsJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), relations.map(ApiRelation(_)))
          )
          relationsJson mustBe expectedJson
      }
    }
    "return forbidden for non admin user" in {
      val env = fakeEnvironment(admin.copy(role = User.Role.User))
      val fixture = getFixture(env)
      val request = authenticated(FakeRequest(), env)
      val response = fixture.controller.getList(None).apply(request)

      status(response) mustBe FORBIDDEN
    }
  }

  "PUT /relations" should {
    "update relations" in {
      forAll { (relation: Relation) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.update(relation)).thenReturn(toSuccessResult(relation))

        val partialRelation = getPartialRelation(relation)
        val request = authenticated(
          FakeRequest("PUT", "/relations")
            .withBody[ApiPartialRelation](partialRelation)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.update(relation.id).apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiRelation(relation))

        status(response) mustBe OK
        responseJson mustBe expectedJson
      }
    }
  }

  private def getPartialRelation(relation: Relation) = {
    ApiPartialRelation(
      relation.project.id,
      relation.groupFrom.id,
      relation.groupTo.map(_.id),
      relation.form.id,
      ApiRelation.Kind(relation.kind),
      relation.templates.map(t =>
        ApiPartialTemplateBinding(t.template.id, ApiNotificationKind(t.kind), ApiNotificationRecipient(t.recipient))
      ),
      relation.canSelfVote,
      relation.canSkipAnswers
    )
  }
  "POST /relations" should {
    "create relations" in {
      forAll { (relation: Relation) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.create(relation.copy(id = 0))).thenReturn(toSuccessResult(relation))

        val partialRelation = getPartialRelation(relation)

        val request = authenticated(
          FakeRequest("POST", "/relations")
            .withBody[ApiPartialRelation](partialRelation)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.create.apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiRelation(relation))

        status(response) mustBe CREATED
        responseJson mustBe expectedJson
      }
    }
  }

  "DELETE /relations" should {
    "delete relations" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.delete(id)).thenReturn(toSuccessResult(()))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.delete(id)(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
