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
import controllers.api.event.ApiEvent
import models.ListWithTotal
import models.event.Event
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.event.EventService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.EventGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

/**
  * Test for user event controller.
  */
class EventControllerTest extends BaseControllerTest with EventGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    eventServiceMock: EventService,
    controller: EventController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val eventServiceMock = mock[EventService]
    val controller = new EventController(silhouette, eventServiceMock, cc, ec)
    TestFixture(silhouette, eventServiceMock, controller)
  }

  private val user = UserFixture.user

  "getById" should {
    "return not found if event not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(user)
        val fixture = getFixture(env)
        when(fixture.eventServiceMock.getByIdWithAuth(id)(user))
          .thenReturn(toErrorResult[Event](NotFoundError.Event(id)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with event" in {
      forAll { (id: Long, event: Event) =>
        val env = fakeEnvironment(user)
        val fixture = getFixture(env)
        when(fixture.eventServiceMock.getByIdWithAuth(id)(user)).thenReturn(toSuccessResult(event))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val eventJson = contentAsJson(response)
        eventJson mustBe Json.toJson(ApiEvent(event)(user))
      }
    }
  }

  "getList" should {
    "return events list from service" in {
      forAll {
        (
          optStatus: Option[Event.Status],
          total: Int,
          events: Seq[Event]
        ) =>
          val env = fakeEnvironment(user)
          val fixture = getFixture(env)
          when(fixture.eventServiceMock.listWithAuth(optStatus)(user))
            .thenReturn(toSuccessResult(ListWithTotal(total, events)))
          val request = authenticated(FakeRequest(), env)

          val response =
            fixture.controller.getList(optStatus.map(ApiEvent.EventStatus(_)))(request)

          status(response) mustBe OK
          val eventsJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), events.map(ApiEvent(_)(user)))
          )
          eventsJson mustBe expectedJson
      }
    }
  }
}
