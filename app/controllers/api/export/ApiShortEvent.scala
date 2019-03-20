package controllers.api.export

import java.time.ZoneOffset

import controllers.api.Response
import controllers.api.event.ApiEvent
import models.event.Event
import play.api.libs.json.Json

/**
  * API event export model.
  */
case class ApiShortEvent(
  id: Long,
  description: Option[String],
  start: Long,
  end: Long,
  status: ApiEvent.EventStatus
) extends Response

object ApiShortEvent {
  implicit val writes = Json.writes[ApiShortEvent]

  def apply(e: Event): ApiShortEvent = ApiShortEvent(
    e.id,
    e.description,
    e.start.atZone(ZoneOffset.UTC).toEpochSecond,
    e.end.atZone(ZoneOffset.UTC).toEpochSecond,
    ApiEvent.EventStatus(e.status)
  )
}
