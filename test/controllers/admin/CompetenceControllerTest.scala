package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.competence.{ApiCompetence, ApiPartialCompetence}
import models.competence.Competence
import models.user.User
import models.{EntityKind, ListWithTotal}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.CompetenceService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.CompetenceGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext

/**
  * Test for competences controller.
  */
class CompetenceControllerTest extends BaseControllerTest with CompetenceGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    competenceService: CompetenceService,
    controller: CompetenceController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val competenceService = mock[CompetenceService]
    val controller = new CompetenceController(silhouette, competenceService, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, competenceService, controller)
  }

  private val admin = UserFixture.admin

  "getById" should {
    "return not found if competence not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceService.getById(id)).thenReturn(toErrorResult[Competence](NotFoundError.Competence(id)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with competence" in {
      forAll { (id: Long, competence: Competence) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceService.getById(id)).thenReturn(toSuccessResult(competence))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val competenceJson = contentAsJson(response)
        competenceJson mustBe Json.toJson(ApiCompetence.fromModel(competence))
      }
    }
  }

  "getList" should {
    "return competence s list from service" in {
      forAll {
        (
          groupId: Option[Long],
          total: Int,
          competences: Seq[Competence]
        ) =>
          val env = fakeEnvironment(admin)
          val fixture = getFixture(env)
          when(fixture.competenceService.getList(groupId)(ListMeta.default))
            .thenReturn(toSuccessResult(ListWithTotal(total, competences)))
          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller.getList(groupId)(request)

          status(response) mustBe OK
          val competencesJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), competences.map(ApiCompetence.fromModel))
          )
          competencesJson mustBe expectedJson
      }
    }
    "return forbidden for non admin user" in {
      val env = fakeEnvironment(admin.copy(role = User.Role.User))
      val fixture = getFixture(env)
      val request = authenticated(FakeRequest(), env)
      val response = fixture.controller.getList(None).apply(request)

      status(response) mustBe FORBIDDEN
    }
  }

  private def getPartialCompetence(c: Competence) = {
    ApiPartialCompetence(
      c.groupId,
      c.name,
      c.description,
      Some(c.machineName)
    )
  }

  "update" should {
    "update competence s" in {
      forAll { (competence: Competence) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceService.update(competence.copy(kind = EntityKind.Template)))
          .thenReturn(toSuccessResult(competence))

        val partialCompetence = getPartialCompetence(competence)
        val request = authenticated(
          FakeRequest()
            .withBody[ApiPartialCompetence](partialCompetence)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.update(competence.id).apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiCompetence.fromModel(competence))

        status(response) mustBe OK
        responseJson mustBe expectedJson
      }
    }
  }

  "create" should {
    "create competence s" in {
      forAll { (competence: Competence) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceService.create(competence.copy(id = 0, kind = EntityKind.Template)))
          .thenReturn(toSuccessResult(competence))

        val partialCompetence = getPartialCompetence(competence)

        val request = authenticated(
          FakeRequest()
            .withBody[ApiPartialCompetence](partialCompetence)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.create.apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiCompetence.fromModel(competence))

        status(response) mustBe CREATED
        responseJson mustBe expectedJson
      }
    }
  }

  "delete" should {
    "delete competence s" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceService.delete(id)).thenReturn(toSuccessResult(()))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.delete(id)(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
