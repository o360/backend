package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.api.Response
import controllers.api.assessment.ApiAssessment
import models.ListWithTotal
import models.assessment.Assessment
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AssessmentService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.AssessmentGenerator
import utils.errors.ApplicationError
import utils.listmeta.ListMeta

import scalaz.{EitherT, \/, \/-}

/**
  * Test for assessment controller.
  */
class AssessmentControllerTest extends BaseControllerTest {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    assessmentServiceMock: AssessmentService,
    controller: AssessmentController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val assessmentServiceMock = mock[AssessmentService]
    val controller = new AssessmentController(silhouette, assessmentServiceMock)
    TestFixture(silhouette, assessmentServiceMock, controller)
  }

  private val admin = UserFixture.admin

  "GET /assessments" should {
    "return assessments list from service" in {
      val env = fakeEnvironment(admin)
      val fixture = getFixture(env)
      val eventId = 1
      val projectId = 2
      val total = 3
      val assessments = Seq(Assessment(None, Nil))
      when(fixture.assessmentServiceMock.getList(eventId, projectId)(admin))
        .thenReturn(EitherT.eitherT(toFuture(\/-(ListWithTotal(total, assessments)): ApplicationError \/ ListWithTotal[Assessment])))
      val request = authenticated(FakeRequest(), env)

      val response = fixture.controller.getList(eventId, projectId)(request)

      status(response) mustBe OK
      val assessmentsJson = contentAsJson(response)
      val expectedJson = Json.toJson(
        Response.List(Response.Meta(total, ListMeta.default), assessments.map(ApiAssessment(_)))
      )
      assessmentsJson mustBe expectedJson
    }
  }
}
