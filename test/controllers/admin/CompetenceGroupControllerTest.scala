package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.competence.{ApiCompetenceGroup, ApiPartialCompetenceGroup}
import models.competence.CompetenceGroup
import models.user.User
import models.{EntityKind, ListWithTotal}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.CompetenceGroupService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.CompetenceGroupGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext

/**
  * Test for competence groups controller.
  */
class CompetenceGroupControllerTest extends BaseControllerTest with CompetenceGroupGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    competenceGroupService: CompetenceGroupService,
    controller: CompetenceGroupController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val competenceGroupService = mock[CompetenceGroupService]
    val controller =
      new CompetenceGroupController(silhouette, competenceGroupService, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, competenceGroupService, controller)
  }

  private val admin = UserFixture.admin

  "getById" should {
    "return not found if competenceGroup not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceGroupService.getById(id))
          .thenReturn(toErrorResult[CompetenceGroup](NotFoundError.CompetenceGroup(id)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with competenceGroup" in {
      forAll { (id: Long, competenceGroup: CompetenceGroup) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceGroupService.getById(id)).thenReturn(toSuccessResult(competenceGroup))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val competenceGroupJson = contentAsJson(response)
        competenceGroupJson mustBe Json.toJson(ApiCompetenceGroup.fromModel(competenceGroup))
      }
    }
  }

  "getList" should {
    "return competence groups list from service" in {
      forAll {
        (
          total: Int,
          competenceGroups: Seq[CompetenceGroup]
        ) =>
          val env = fakeEnvironment(admin)
          val fixture = getFixture(env)
          when(fixture.competenceGroupService.getList(ListMeta.default))
            .thenReturn(toSuccessResult(ListWithTotal(total, competenceGroups)))
          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller.getList(request)

          status(response) mustBe OK
          val competenceGroupsJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), competenceGroups.map(ApiCompetenceGroup.fromModel))
          )
          competenceGroupsJson mustBe expectedJson
      }
    }
    "return forbidden for non admin user" in {
      val env = fakeEnvironment(admin.copy(role = User.Role.User))
      val fixture = getFixture(env)
      val request = authenticated(FakeRequest(), env)
      val response = fixture.controller.getList.apply(request)

      status(response) mustBe FORBIDDEN
    }
  }

  private def getPartialCompetenceGroup(cg: CompetenceGroup) = {
    ApiPartialCompetenceGroup(
      cg.name,
      cg.description,
      Some(cg.machineName)
    )
  }

  "update" should {
    "update competence groups" in {
      forAll { (competenceGroup: CompetenceGroup) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceGroupService.update(competenceGroup.copy(kind = EntityKind.Template)))
          .thenReturn(toSuccessResult(competenceGroup))

        val partialCompetenceGroup = getPartialCompetenceGroup(competenceGroup)
        val request = authenticated(
          FakeRequest()
            .withBody[ApiPartialCompetenceGroup](partialCompetenceGroup)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.update(competenceGroup.id).apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiCompetenceGroup.fromModel(competenceGroup))

        status(response) mustBe OK
        responseJson mustBe expectedJson
      }
    }
  }

  "create" should {
    "create competence groups" in {
      forAll { (competenceGroup: CompetenceGroup) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceGroupService.create(competenceGroup.copy(id = 0, kind = EntityKind.Template)))
          .thenReturn(toSuccessResult(competenceGroup))

        val partialCompetenceGroup = getPartialCompetenceGroup(competenceGroup)

        val request = authenticated(
          FakeRequest()
            .withBody[ApiPartialCompetenceGroup](partialCompetenceGroup)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.create.apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiCompetenceGroup.fromModel(competenceGroup))

        status(response) mustBe CREATED
        responseJson mustBe expectedJson
      }
    }
  }

  "delete" should {
    "delete competence groups" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.competenceGroupService.delete(id)).thenReturn(toSuccessResult(()))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.delete(id)(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
