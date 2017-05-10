package controllers.api.event

import java.sql.Timestamp

import controllers.api.TimestampFormat._
import models.event.Event
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * Partial api model for event.
  */
case class ApiPartialEvent(
  description: Option[String],
  start: Timestamp,
  end: Timestamp,
  canRevote: Boolean,
  notifications: Seq[ApiEvent.NotificationTime]
) {
  /**
    * Converts api model to model.
    *
    * @param id target id
    */
  def toModel(id: Long = 0) = Event(
    id,
    description,
    start,
    end,
    canRevote,
    notifications.map(_.toModel)
  )
}

object ApiPartialEvent {
  implicit val partialEventReads: Reads[ApiPartialEvent] = (
    (__ \ "description").readNullable[String](maxLength(1024)) and
      (__ \ "start").read[Timestamp] and
      (__ \ "end").read[Timestamp] and
      (__ \ "canRevote").read[Boolean] and
      (__ \ "notifications").read[Seq[ApiEvent.NotificationTime]]
    ) (ApiPartialEvent.apply _)
}
