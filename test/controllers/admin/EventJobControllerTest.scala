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
import services.event.EventJobService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture

/**
  * Test for user eventJobs controller.
  */
class EventJobControllerTest extends BaseControllerTest {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    eventJobService: EventJobService,
    controller: EventJobController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val eventJobService = mock[EventJobService]
    val controller = new EventJobController(silhouette, eventJobService, cc, ec)
    TestFixture(silhouette, eventJobService, controller)
  }

  private val admin = UserFixture.admin

  "restart" should {
    "restart job" in {
      forAll { jobId: Long =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventJobService.runFailedJob(jobId)).thenReturn(toSuccessResult(()))

        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.restart(jobId).apply(request)

        status(response) mustBe NO_CONTENT
      }
    }
  }
}
