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

package controllers.user

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.project.ApiActiveProject
import models.ListWithTotal
import models.project.ActiveProject
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.event.ActiveProjectService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.ActiveProjectGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext

/**
  * Test for active project controller.
  */
class ActiveProjectControllerTest extends BaseControllerTest with ActiveProjectGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    activeProjectService: ActiveProjectService,
    controller: ActiveProjectController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val activeProjectService = mock[ActiveProjectService]
    val controller =
      new ActiveProjectController(silhouette, activeProjectService, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, activeProjectService, controller)
  }

  private val user = UserFixture.user

  "getById" should {
    "return not found if activeProject not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(user)
        val fixture = getFixture(env)
        when(fixture.activeProjectService.getById(id)(user))
          .thenReturn(toErrorResult[ActiveProject](NotFoundError.ActiveProject(id)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with activeProject" in {
      forAll { (id: Long, activeProject: ActiveProject) =>
        val env = fakeEnvironment(user)
        val fixture = getFixture(env)
        when(fixture.activeProjectService.getById(id)(user)).thenReturn(toSuccessResult(activeProject))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val activeProjectJson = contentAsJson(response)
        activeProjectJson mustBe Json.toJson(ApiActiveProject(activeProject))
      }
    }
  }

  "getList" should {
    "return activeProject s list from service" in {
      forAll {
        (
          eventId: Option[Long],
          total: Int,
          activeProjects: Seq[ActiveProject]
        ) =>
          val env = fakeEnvironment(user)
          val fixture = getFixture(env)
          when(fixture.activeProjectService.getList(eventId)(user, ListMeta.default))
            .thenReturn(toSuccessResult(ListWithTotal(total, activeProjects)))
          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller.getList(eventId)(request)

          status(response) mustBe OK
          val activeProjectsJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), activeProjects.map(ApiActiveProject(_)))
          )
          activeProjectsJson mustBe expectedJson
      }
    }
  }
}
