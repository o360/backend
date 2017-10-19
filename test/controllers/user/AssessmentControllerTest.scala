package controllers.user

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.assessment.{ApiAssessment, ApiPartialAssessment, ApiPartialFormAnswer}
import models.ListWithTotal
import models.assessment.{Assessment, PartialAnswer, PartialAssessment}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AssessmentService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext

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
    val controller = new AssessmentController(silhouette, assessmentServiceMock, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, assessmentServiceMock, controller)
  }

  private val admin = UserFixture.admin

  "GET /assessments" should {
    "return assessments list from service" in {
      val env = fakeEnvironment(admin)
      val fixture = getFixture(env)
      val projectId = 2
      val total = 3
      val assessments = Seq(Assessment(None, Nil))
      when(fixture.assessmentServiceMock.getList(projectId)(admin))
        .thenReturn(toSuccessResult(ListWithTotal(total, assessments)))
      val request = authenticated(FakeRequest(), env)

      val response = fixture.controller.getList(projectId)(request)

      status(response) mustBe OK
      val assessmentsJson = contentAsJson(response)
      val expectedJson = Json.toJson(
        Response.List(Response.Meta(total, ListMeta.default), assessments.map(ApiAssessment(_)))
      )
      assessmentsJson mustBe expectedJson
    }
  }

  "POST /assessments" should {
    "submit answers" in {
      val env = fakeEnvironment(admin)
      val fixture = getFixture(env)

      val assessment = PartialAssessment(Some(admin.id), Seq(PartialAnswer(1, false, Set())))
      when(fixture.assessmentServiceMock.bulkSubmit(1, Seq(assessment))(admin)).thenReturn(toSuccessResult(()))

      val partialAssessment = ApiPartialAssessment(Some(1), ApiPartialFormAnswer(1, Seq(), false, false))

      val request = authenticated(
        FakeRequest("POST", "/assessments")
          .withBody[Seq[ApiPartialAssessment]](Seq(partialAssessment))
          .withHeaders(CONTENT_TYPE -> "application/json"),
        env
      )

      val response = fixture.controller.bulkSubmit(1).apply(request)
      status(response) mustBe NO_CONTENT
    }
  }
}
