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
import controllers.api.report.ApiReport
import models.report.SimpleReport
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ReportService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.SimpleReportGenerator

/**
  * Test for report controller.
  */
class ReportControllerTest extends BaseControllerTest with SimpleReportGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    reportService: ReportService,
    controller: ReportController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val reportService = mock[ReportService]
    val controller = new ReportController(silhouette, reportService, cc, ec)
    TestFixture(silhouette, reportService, controller)
  }

  private val user = UserFixture.user

  "getAuditorReport" should {
    "return auditor report" in {
      forAll { (simpleReports: Seq[SimpleReport], projectId: Long) =>
        val env = fakeEnvironment(user)
        val fixture = getFixture(env)
        when(fixture.reportService.getAuditorReport(projectId)(user)).thenReturn(toSuccessResult(simpleReports))

        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getAuditorReport(projectId)(request)

        status(response) mustBe OK
        val reportJson = contentAsJson(response)
        reportJson mustBe Json.toJson(Response.List(simpleReports.map(ApiReport(_))))
      }
    }
  }
}
