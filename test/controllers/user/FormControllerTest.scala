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
import utils.errors.ApplicationError

import scala.concurrent.ExecutionContext
import scalaz._

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
        when(fixture.formServiceMock.userGetById(id)(user))
          .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val formJson = contentAsJson(response)
        formJson mustBe Json.toJson(ApiForm(form))
      }
    }
  }
}
