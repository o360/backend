package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EventProjectService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.TristateGenerator
import utils.errors.{ApplicationError, NotFoundError}

import scalaz.{-\/, \/, \/-, EitherT}

/**
  * Test for event-project controller.
  */
class EventProjectControllerTest extends BaseControllerTest with TristateGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    eventProjectServiceMock: EventProjectService,
    controller: EventProjectController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val eventProjectService = mock[EventProjectService]
    val controller = new EventProjectController(silhouette, eventProjectService, cc, ec)
    TestFixture(silhouette, eventProjectService, controller)
  }

  private val admin = UserFixture.admin

  "POST /events/id/projects/id" should {
    "return error if error happend" in {
      forAll { (eventId: Long, projectId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventProjectServiceMock.add(eventId, projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Event(eventId)): ApplicationError \/ Unit)))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.create(eventId, projectId).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { (eventId: Long, projectId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventProjectServiceMock.add(eventId, projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(()): ApplicationError \/ Unit)))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.create(eventId, projectId).apply(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }

  "DELETE /events/id/projects/id" should {
    "return error if error happend" in {
      forAll { (eventId: Long, projectId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventProjectServiceMock.remove(eventId, projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Event(eventId)): ApplicationError \/ Unit)))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.delete(eventId, projectId).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { (eventId: Long, projectId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventProjectServiceMock.remove(eventId, projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(()): ApplicationError \/ Unit)))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.delete(eventId, projectId).apply(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
