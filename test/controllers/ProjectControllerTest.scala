package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.api.Response
import controllers.api.project.{ApiPartialProject, ApiProject}
import models.ListWithTotal
import models.project.Project
import models.user.User
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ProjectService
import silhouette.DefaultEnv
import testutils.generator.ProjectGenerator
import utils.errors.{ApplicationError, NotFoundError}
import utils.listmeta.ListMeta

import scalaz.{-\/, EitherT, \/, \/-}

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
    val controller = new ProjectController(silhouette, projectServiceMock)
    TestFixture(silhouette, projectServiceMock, controller)
  }

  private val admin = User(1, None, None, User.Role.Admin, User.Status.Approved)

  "GET /projects/id" should {
    "return not found if project not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.getById(id)(admin))
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
        when(fixture.projectServiceMock.getById(id)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(project): ApplicationError \/ Project)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val projectJson = contentAsJson(response)
        projectJson mustBe Json.toJson(ApiProject(project))
      }
    }
  }

  "GET /projects" should {
    "return projects list from service" in {
      forAll { (
      total: Int,
      projects: Seq[Project]
      ) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.getList()(admin, ListMeta.default))
          .thenReturn(EitherT.eitherT(toFuture(\/-(ListWithTotal(total, projects)): ApplicationError \/ ListWithTotal[Project])))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getList()(request)

        status(response) mustBe OK
        val projectsJson = contentAsJson(response)
        val expectedJson = Json.toJson(
          Response.List(Response.Meta(total, ListMeta.default), projects.map(ApiProject(_)))
        )
        projectsJson mustBe expectedJson
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

  "PUT /projects" should {
    "update projects" in {
      forAll { (project: Project) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.projectServiceMock.update(project)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(project): ApplicationError \/ Project)))

        val partialProject = ApiPartialProject(project)
        val request = authenticated(
          FakeRequest("PUT", "/projects")
            .withBody[ApiPartialProject](partialProject)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.update(project.id).apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiProject(project))

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
        when(fixture.projectServiceMock.create(project.copy(id = 0))(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(project): ApplicationError \/ Project)))

        val partialProject = ApiPartialProject(project)
        val request = authenticated(
          FakeRequest("POST", "/projects")
            .withBody[ApiPartialProject](partialProject)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.create.apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiProject(project))

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
