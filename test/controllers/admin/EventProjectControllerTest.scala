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
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.event.EventProjectService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.TristateGenerator
import utils.errors.NotFoundError

/**
  * Test for event-project controller.
  */
class EventProjectControllerTest extends BaseControllerTest with TristateGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    eventProjectServiceMock: EventProjectService,
    controller: EventProjectController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val eventProjectService = mock[EventProjectService]
    val controller = new EventProjectController(silhouette, eventProjectService, cc, ec)
    TestFixture(silhouette, eventProjectService, controller)
  }

  private val admin = UserFixture.admin

  "POST /events/id/projects/id" should {
    "return error if error happend" in {
      forAll { (eventId: Long, projectId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventProjectServiceMock.add(eventId, projectId))
          .thenReturn(toErrorResult[Unit](NotFoundError.Event(eventId)))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.create(eventId, projectId).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { (eventId: Long, projectId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventProjectServiceMock.add(eventId, projectId)).thenReturn(toSuccessResult(()))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.create(eventId, projectId).apply(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }

  "DELETE /events/id/projects/id" should {
    "return error if error happend" in {
      forAll { (eventId: Long, projectId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventProjectServiceMock.remove(eventId, projectId))
          .thenReturn(toErrorResult[Unit](NotFoundError.Event(eventId)))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.delete(eventId, projectId).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { (eventId: Long, projectId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventProjectServiceMock.remove(eventId, projectId)).thenReturn(toSuccessResult(()))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.delete(eventId, projectId).apply(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
