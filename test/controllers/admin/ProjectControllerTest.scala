package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import controllers.api.project.{ApiPartialProject, ApiPartialTemplateBinding, ApiProject}
import models.ListWithTotal
import models.project.Project
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ProjectService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.ProjectGenerator
import utils.errors.{ApplicationError, NotFoundError}
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext
import scalaz.{-\/, \/, \/-, EitherT}

/**
  * Test for projects controller.
  */
class ProjectControllerTest extends BaseControllerTest with ProjectGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    projectServiceMock: ProjectService,
    controller: ProjectController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val projectServiceMock = mock[ProjectService]
    val controller = new ProjectController(silhouette, projectServiceMock, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, projectServiceMock, controller)
  }

  private val admin = UserFixture.admin

  "GET /projects/id" should {
    "return not found if project not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.getById(id))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Project(id)): ApplicationError \/ Project)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with project" in {
      forAll { (id: Long, project: Project) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.getById(id))
          .thenReturn(EitherT.eitherT(toFuture(\/-(project): ApplicationError \/ Project)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val projectJson = contentAsJson(response)
        projectJson mustBe Json.toJson(ApiProject(project)(UserFixture.admin))
      }
    }
  }

  "GET /projects" should {
    "return projects list from service" in {
      forAll {
        (
          eventId: Option[Long],
          groupId: Option[Long],
          total: Int,
          projects: Seq[Project]
        ) =>
          val env = fakeEnvironment(admin)
          val fixture = getFixture(env)
          when(fixture.projectServiceMock.getList(eventId, groupId)(ListMeta.default))
            .thenReturn(EitherT.eitherT(
              toFuture(\/-(ListWithTotal(total, projects)): ApplicationError \/ ListWithTotal[Project])))
          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller.getList(eventId, groupId)(request)

          status(response) mustBe OK
          val projectsJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), projects.map(ApiProject(_)(UserFixture.admin)))
          )
          projectsJson mustBe expectedJson
      }
    }
  }

  def toPartialProject(project: Project) = ApiPartialProject(
    project.name,
    project.description,
    project.groupAuditor.id,
    project.templates.map { t =>
      ApiPartialTemplateBinding(t.template.id, ApiNotificationKind(t.kind), ApiNotificationRecipient(t.recipient))
    },
    project.formsOnSamePage,
    project.canRevote,
    project.isAnonymous,
    Some(project.machineName)
  )

  "PUT /projects" should {
    "update projects" in {
      forAll { (project: Project) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.update(project))
          .thenReturn(EitherT.eitherT(toFuture(\/-(project): ApplicationError \/ Project)))

        val partialProject = toPartialProject(project)
        val request = authenticated(
          FakeRequest("PUT", "/projects")
            .withBody[ApiPartialProject](partialProject)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.update(project.id).apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiProject(project)(UserFixture.admin))

        status(response) mustBe OK
        responseJson mustBe expectedJson
      }
    }
  }

  "POST /projects" should {
    "create projects" in {
      forAll { (project: Project) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.create(project.copy(id = 0)))
          .thenReturn(EitherT.eitherT(toFuture(\/-(project): ApplicationError \/ Project)))

        val partialProject = toPartialProject(project)
        val request = authenticated(
          FakeRequest("POST", "/projects")
            .withBody[ApiPartialProject](partialProject)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.create.apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiProject(project)(UserFixture.admin))

        status(response) mustBe CREATED
        responseJson mustBe expectedJson
      }
    }
  }

  "DELETE /projects" should {
    "delete projects" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.delete(id)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(()): ApplicationError \/ Unit)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.delete(id)(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
