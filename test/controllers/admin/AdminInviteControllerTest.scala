/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.invite.{ApiInvite, ApiPartialInvite}
import models.invite.Invite
import models.{ListWithTotal, NamedEntity}
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status, _}
import services.InviteService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.InviteGenerator
import utils.listmeta.ListMeta

/**
  * Admin invite controller test.
  */
class AdminInviteControllerTest extends BaseControllerTest with InviteGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    inviteService: InviteService,
    controller: InviteController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val inviteService = mock[InviteService]
    val controller = new InviteController(silhouette, inviteService, cc, ec)
    TestFixture(silhouette, inviteService, controller)
  }

  "getList" should {
    "return list" in {
      forAll {
        (
          activated: Option[Boolean],
          total: Int,
          invites: Seq[Invite]
        ) =>
          val env = fakeEnvironment(UserFixture.admin)
          val fixture = getFixture(env)
          when(fixture.inviteService.getList(activated)(ListMeta.default))
            .thenReturn(toSuccessResult(ListWithTotal(total, invites)))

          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller.getList(activated)(request)

          status(response) mustBe OK
          val invitesJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), invites.map(ApiInvite(_)(UserFixture.admin)))
          )
          invitesJson mustBe expectedJson
      }
    }
  }

  "bulkCreate" should {
    "bulk create invites" in {
      val env = fakeEnvironment(UserFixture.admin)
      val fixture = getFixture(env)
      val invite = Invite("email", Set(NamedEntity(1), NamedEntity(2), NamedEntity(3)))
      when(fixture.inviteService.createInvites(*)).thenReturn(toSuccessResult(()))

      val partialInvite = ApiPartialInvite(
        invite.email,
        invite.groups.map(_.id)
      )
      val request = authenticated(
        FakeRequest()
          .withBody[Seq[ApiPartialInvite]](Seq(partialInvite))
          .withHeaders(CONTENT_TYPE -> "application/json"),
        env
      )

      val response = fixture.controller.bulkCreate.apply(request)

      status(response) mustBe NO_CONTENT
    }
  }
}
