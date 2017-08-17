package controllers

import controllers.api.export.ApiExportCode
import models.ListWithTotal
import models.event.Event
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EventResultExportService
import utils.Config

import scala.concurrent.ExecutionContext

/**
  * Test for EventResultExportController.
  */
class EventResultExportControllerTest extends BaseControllerTest {

  private case class Fixture(
    config: Config,
    exportService: EventResultExportService,
    controller: EventResultExportController
  )

  private def getFixture = {
    val config = mock[Config]
    val exportService = mock[EventResultExportService]
    val controller = new EventResultExportController(config, exportService, cc, ExecutionContext.Implicits.global)
    Fixture(config, exportService, controller)
  }

  "POST /export/events" should {
    "return 403 if incorrect code" in {
      val fixture = getFixture
      when(fixture.config.exportSecret).thenReturn("valid-secret")

      val request = FakeRequest().withBody(ApiExportCode("not-valid-secret"))
      val response = fixture.controller.listEvents().apply(request)

      status(response) mustBe 403
    }

    "return list of events" in {
      val fixture = getFixture
      when(fixture.config.exportSecret).thenReturn("valid-secret")
      when(fixture.exportService.eventsList).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))

      val request = FakeRequest().withBody(ApiExportCode("valid-secret"))
      val response = fixture.controller.listEvents().apply(request)

      status(response) mustBe 200
    }
  }

  "POST /export/event" should {
    "return 403 if incorrect code" in {
      val fixture = getFixture
      when(fixture.config.exportSecret).thenReturn("valid-secret")

      val request = FakeRequest().withBody(ApiExportCode("not-valid-secret"))
      val response = fixture.controller.export(123).apply(request)

      status(response) mustBe 403
    }

    "return list of answers" in {
      val fixture = getFixture
      when(fixture.config.exportSecret).thenReturn("valid-secret")
      when(fixture.exportService.exportAnswers(123)).thenReturn(toFuture((Seq.empty, Seq.empty, Seq.empty)))

      val request = FakeRequest().withBody(ApiExportCode("valid-secret"))
      val response = fixture.controller.export(123).apply(request)

      status(response) mustBe 200
    }
  }
}
