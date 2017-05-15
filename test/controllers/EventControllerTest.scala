package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.api.Response
import controllers.api.event.{ApiPartialEvent, ApiEvent}
import models.ListWithTotal
import models.event.Event
import models.user.User
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EventService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.EventGenerator
import utils.errors.{ApplicationError, NotFoundError}
import utils.listmeta.ListMeta

import scalaz.{-\/, EitherT, \/, \/-}

/**
  * Test for event controller.
  */
class EventControllerTest extends BaseControllerTest with EventGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    eventServiceMock: EventService,
    controller: EventController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val eventServiceMock = mock[EventService]
    val controller = new EventController(silhouette, eventServiceMock)
    TestFixture(silhouette, eventServiceMock, controller)
  }

  private val admin = UserFixture.admin

  "GET /events/id" should {
    "return not found if event not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventServiceMock.getById(id)(admin))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Event(id)): ApplicationError \/ Event)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with event" in {
      forAll { (id: Long, event: Event) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventServiceMock.getById(id)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(event): ApplicationError \/ Event)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val eventJson = contentAsJson(response)
        eventJson mustBe Json.toJson(ApiEvent(event)(UserFixture.admin))
      }
    }
  }

  "GET /events" should {
    "return events list from service" in {
      forAll { (
      projectId: Option[Long],
        optStatus: Option[Event.Status],
      total: Int,
      events: Seq[Event]
      ) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventServiceMock.list(optStatus, projectId)(admin, ListMeta.default))
          .thenReturn(EitherT.eitherT(toFuture(\/-(ListWithTotal(total, events)): ApplicationError \/ ListWithTotal[Event])))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getList(optStatus.map(ApiEvent.EventStatus(_)), projectId)(request)

        status(response) mustBe OK
        val eventsJson = contentAsJson(response)
        val expectedJson = Json.toJson(
          Response.List(Response.Meta(total, ListMeta.default), events.map(ApiEvent(_)(UserFixture.admin)))
        )
        eventsJson mustBe expectedJson
      }
    }
  }

  "PUT /events" should {
    "update events" in {
      forAll { (event: Event) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventServiceMock.update(event)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(event): ApplicationError \/ Event)))

        val partialEvent = ApiPartialEvent(
          event.description,
          event.start,
          event.end,
          event.canRevote,
          event.notifications.map(ApiEvent.NotificationTime(_))

        )
        val request = authenticated(
          FakeRequest("PUT", "/events")
            .withBody[ApiPartialEvent](partialEvent)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.update(event.id).apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiEvent(event)(UserFixture.admin))

        status(response) mustBe OK
        responseJson mustBe expectedJson
      }
    }
  }

  "POST /events" should {
    "create events" in {
      forAll { (event: Event) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventServiceMock.create(event.copy(id = 0))(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(event): ApplicationError \/ Event)))

        val partialEvent = ApiPartialEvent(
          event.description,
          event.start,
          event.end,
          event.canRevote,
          event.notifications.map(ApiEvent.NotificationTime(_))

        )

        val request = authenticated(
          FakeRequest("POST", "/events")
            .withBody[ApiPartialEvent](partialEvent)
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.create.apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiEvent(event)(UserFixture.admin))

        status(response) mustBe CREATED
        responseJson mustBe expectedJson
      }
    }
  }

  "DELETE /events" should {
    "delete events" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.eventServiceMock.delete(id)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(()): ApplicationError \/ Unit)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.delete(id)(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
