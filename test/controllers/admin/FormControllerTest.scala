package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.form.{ApiForm, ApiPartialForm}
import models.ListWithTotal
import models.form.{Form, FormShort}
import models.user.User
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.FormService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.FormGenerator
import utils.errors.{ApplicationError, NotFoundError}
import utils.listmeta.ListMeta

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

  private val admin = UserFixture.admin

  "GET /forms/id" should {
    "return not found if form not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.formServiceMock.getById(id))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Form(id)): ApplicationError \/ Form)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with form" in {
      forAll { (id: Long, form: Form) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.formServiceMock.getById(id))
          .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val formJson = contentAsJson(response)
        formJson mustBe Json.toJson(ApiForm(form))
      }
    }
  }

  "GET /forms" should {
    "return forms without elements from service" in {
      forAll { (total: Int, forms: Seq[FormShort]) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.formServiceMock.getList()(ListMeta.default))
          .thenReturn(
            EitherT.eitherT(toFuture(\/-(ListWithTotal(total, forms)): ApplicationError \/ ListWithTotal[FormShort])))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getList()(request)

        status(response) mustBe OK
        val formsJson = contentAsJson(response)
        val expectedJson = Json.toJson(
          Response.List(Response.Meta(total, ListMeta.default), forms.map(ApiForm(_)))
        )
        formsJson mustBe expectedJson
      }
    }
    "return forbidden for non admin user" in {
      val env = fakeEnvironment(admin.copy(role = User.Role.User))
      val fixture = getFixture(env)
      val request = authenticated(FakeRequest(), env)
      val response = fixture.controller.getList().apply(request)

      status(response) mustBe FORBIDDEN
    }
  }

  private def toApiPartialForm(form: Form) = ApiPartialForm(
    form.name,
    Some(form.elements.map { element =>
      ApiPartialForm.Element(ApiForm.ApiElementKind(element.kind),
                             element.caption,
                             element.required,
                             Some(element.values.map(value => ApiPartialForm.ElementValue(value.caption))))
    }),
    form.showInAggregation,
    Some(form.machineName)
  )

  "PUT /forms" should {
    "update forms" in {
      forAll { (form: Form) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.formServiceMock.update(form.copy(kind = Form.Kind.Active)))
          .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))

        val apiForm = toApiPartialForm(form)
        val request = authenticated(
          FakeRequest("PUT", "/forms")
            .withBody[ApiPartialForm](apiForm)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.update(form.id).apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiForm(form))

        status(response) mustBe OK
        responseJson mustBe expectedJson
      }
    }
  }

  "POST /forms" should {
    "create forms" in {
      forAll { (form: Form) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.formServiceMock.create(form.copy(kind = Form.Kind.Active)))
          .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))

        val apiForm = toApiPartialForm(form)

        val request = authenticated(
          FakeRequest("POST", "/forms")
            .withBody[ApiPartialForm](apiForm)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.create.apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiForm(form))

        status(response) mustBe CREATED
        responseJson mustBe expectedJson
      }
    }
  }

  "DELETE /forms/id" should {
    "delete forms" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.formServiceMock.delete(id))
          .thenReturn(EitherT.eitherT(toFuture(\/-(()): ApplicationError \/ Unit)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.delete(id)(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }

  "POST /forms/id/clone" should {
    "return 404 if form not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.formServiceMock.getById(id))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Form(id)): ApplicationError \/ Form)))

        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.cloneForm(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "clone form" in {
      forAll { (id: Long, form: Form) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.formServiceMock.getById(id))
          .thenReturn(EitherT.eitherT(toFuture(\/-(form.copy(id = id)): ApplicationError \/ Form)))
        when(fixture.formServiceMock.create(form.copy(id = 0)))
          .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.cloneForm(id)(request)
        status(response) mustBe CREATED
        val formJson = contentAsJson(response)
        formJson mustBe Json.toJson(ApiForm(form))
      }
    }
  }
}
