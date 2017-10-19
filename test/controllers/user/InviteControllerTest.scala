package controllers.user

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.invite.ApiInviteCode
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.InviteService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture

/**
  * Test for user invites controller.
  */
class InviteControllerTest extends BaseControllerTest {

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

  private val user = UserFixture.user

  "submit" should {
    "submit code" in {
      forAll { code: String =>
        val env = fakeEnvironment(user)
        val fixture = getFixture(env)
        when(fixture.inviteService.applyInvite(code)(user)).thenReturn(toSuccessResult(()))

        val apiInviteCode = ApiInviteCode(code)

        val request = authenticated(
          FakeRequest()
            .withBody[ApiInviteCode](apiInviteCode)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.submit.apply(request)

        status(response) mustBe NO_CONTENT
      }
    }
  }
}
