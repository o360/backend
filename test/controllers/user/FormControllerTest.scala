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
import controllers.api.form.ApiForm
import models.form.Form
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.FormService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.FormGenerator

import scala.concurrent.ExecutionContext

/**
  * Test for forms controller.
  */
class FormControllerTest extends BaseControllerTest with FormGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    formServiceMock: FormService,
    controller: FormController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val formServiceMock = mock[FormService]
    val controller = new FormController(silhouette, formServiceMock, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, formServiceMock, controller)
  }

  private val user = UserFixture.user

  "GET /forms/id" should {
    "return json with form" in {
      forAll { (id: Long, form: Form) =>
        val env = fakeEnvironment(user)
        val fixture = getFixture(env)
        when(fixture.formServiceMock.getByIdWithAuth(id)(user)).thenReturn(toSuccessResult(form))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val formJson = contentAsJson(response)
        formJson mustBe Json.toJson(ApiForm(form))
      }
    }
  }
}
