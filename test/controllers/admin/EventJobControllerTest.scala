package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.event.EventJobService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture

/**
  * Test for user eventJobs controller.
  */
class EventJobControllerTest extends BaseControllerTest {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    eventJobService: EventJobService,
    controller: EventJobController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val eventJobService = mock[EventJobService]
    val controller = new EventJobController(silhouette, eventJobService, cc, ec)
    TestFixture(silhouette, eventJobService, controller)
  }

  private val admin = UserFixture.admin

  "restart" should {
    "restart job" in {
      forAll { jobId: Long =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventJobService.runFailedJob(jobId)).thenReturn(toSuccessResult(()))

        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.restart(jobId).apply(request)

        status(response) mustBe NO_CONTENT
      }
    }
  }
}
