package controllers.user

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.group.ApiGroup
import models.ListWithTotal
import models.group.Group
import models.user.User
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{GroupService, UserService}
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.GroupGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext

/**
  * Test for user group controller.
  */
class GroupControllerTest extends BaseControllerTest with GroupGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    groupService: GroupService,
    userService: UserService,
    controller: GroupController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val groupService = mock[GroupService]
    val userService = mock[UserService]
    val controller = new GroupController(silhouette, groupService, userService, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, groupService, userService, controller)
  }

  private val user = UserFixture.user

  "getListByUserId" should {
    "return not found if user not found" in {
      forAll { (userId: Long) =>
        val env = fakeEnvironment(user)
        val fixture = getFixture(env)
        when(fixture.userService.getByIdWithAuth(userId)(user))
          .thenReturn(toErrorResult[User](NotFoundError.User(userId)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getListByUserId(userId).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with groups" in {
      forAll {
        (
          userId: Long,
          total: Int,
          groups: Seq[Group]
        ) =>
          val env = fakeEnvironment(user)
          val fixture = getFixture(env)
          when(fixture.userService.getByIdWithAuth(userId)(user)).thenReturn(toSuccessResult(user))
          when(fixture.groupService.listByUserId(userId)).thenReturn(toSuccessResult(ListWithTotal(total, groups)))
          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller.getListByUserId(userId)(request)
          status(response) mustBe OK
          val groupsJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), groups.map(ApiGroup(_)))
          )
          groupsJson mustBe expectedJson
      }
    }
  }

  "getList" should {
    "return groups of logged in user" in {
      forAll {
        (
          total: Int,
          groups: Seq[Group]
        ) =>
          val env = fakeEnvironment(user)
          val fixture = getFixture(env)
          when(fixture.userService.getByIdWithAuth(user.id)(user)).thenReturn(toSuccessResult(user))
          when(fixture.groupService.listByUserId(user.id)).thenReturn(toSuccessResult(ListWithTotal(total, groups)))
          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller.getList(request)
          status(response) mustBe OK
          val groupsJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), groups.map(ApiGroup(_)))
          )
          groupsJson mustBe expectedJson
      }
    }
  }
}
