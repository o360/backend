package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import controllers.api.template.{ApiPartialTemplate, ApiTemplate}
import models.ListWithTotal
import models.notification._
import models.template.Template
import models.user.User
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.TemplateService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.TemplateGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext

/**
  * Test for templates controller.
  */
class TemplateControllerTest extends BaseControllerTest with TemplateGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    templateServiceMock: TemplateService,
    controller: TemplateController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val templateServiceMock = mock[TemplateService]
    val controller = new TemplateController(silhouette, templateServiceMock, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, templateServiceMock, controller)
  }

  private val admin = UserFixture.admin

  "GET /templates/id" should {
    "return not found if template not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.templateServiceMock.getById(id)).thenReturn(toErrorResult[Template](NotFoundError.Template(id)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with template" in {
      forAll { (id: Long, template: Template) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.templateServiceMock.getById(id)).thenReturn(toSuccessResult(template))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val templateJson = contentAsJson(response)
        templateJson mustBe Json.toJson(ApiTemplate(template))
      }
    }
  }

  "GET /templates" should {
    "return templates list from service" in {
      forAll {
        (
          kind: Option[NotificationKind],
          recipient: Option[NotificationRecipient],
          total: Int,
          templates: Seq[Template]
        ) =>
          val env = fakeEnvironment(admin)
          val fixture = getFixture(env)
          when(fixture.templateServiceMock.getList(kind, recipient)(ListMeta.default))
            .thenReturn(toSuccessResult(ListWithTotal(total, templates)))
          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller
            .getList(kind.map(ApiNotificationKind(_)), recipient.map(ApiNotificationRecipient(_)))(request)

          status(response) mustBe OK
          val templatesJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), templates.map(ApiTemplate(_)))
          )
          templatesJson mustBe expectedJson
      }
    }
    "return forbidden for non admin user" in {
      val env = fakeEnvironment(admin.copy(role = User.Role.User))
      val fixture = getFixture(env)
      val request = authenticated(FakeRequest(), env)
      val response = fixture.controller.getList(None, None).apply(request)

      status(response) mustBe FORBIDDEN
    }
  }

  "PUT /templates" should {
    "update templates" in {
      forAll { (template: Template) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.templateServiceMock.update(template)).thenReturn(toSuccessResult(template))

        val partialTemplate = ApiPartialTemplate(
          template.name,
          template.subject,
          template.body,
          ApiNotificationKind(template.kind),
          ApiNotificationRecipient(template.recipient)
        )
        val request = authenticated(
          FakeRequest("PUT", "/templates")
            .withBody[ApiPartialTemplate](partialTemplate)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.update(template.id).apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiTemplate(template))

        status(response) mustBe OK
        responseJson mustBe expectedJson
      }
    }
  }

  "POST /templates" should {
    "create templates" in {
      forAll { (template: Template) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.templateServiceMock.create(template.copy(id = 0))).thenReturn(toSuccessResult(template))

        val partialTemplate = ApiPartialTemplate(
          template.name,
          template.subject,
          template.body,
          ApiNotificationKind(template.kind),
          ApiNotificationRecipient(template.recipient)
        )
        val request = authenticated(
          FakeRequest("POST", "/templates")
            .withBody[ApiPartialTemplate](partialTemplate)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.create.apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiTemplate(template))

        status(response) mustBe CREATED
        responseJson mustBe expectedJson
      }
    }
  }

  "DELETE /templates" should {
    "delete templates" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.templateServiceMock.delete(id)).thenReturn(toSuccessResult(()))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.delete(id)(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }

  "POST /templates/id/clone" should {
    "return 404 if template not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.templateServiceMock.getById(id)).thenReturn(toErrorResult[Template](NotFoundError.Template(id)))

        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.cloneTemplate(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "clone template" in {
      forAll { (id: Long, template: Template) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.templateServiceMock.getById(id)).thenReturn(toSuccessResult(template.copy(id = id)))
        when(fixture.templateServiceMock.create(template.copy(id = 0))).thenReturn(toSuccessResult(template))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.cloneTemplate(id)(request)
        status(response) mustBe CREATED
        val templateJson = contentAsJson(response)
        templateJson mustBe Json.toJson(ApiTemplate(template))
      }
    }
  }
}
