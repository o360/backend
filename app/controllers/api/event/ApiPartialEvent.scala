package controllers.api.event

import java.time.LocalDateTime

import models.event.Event
import models.user.User
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.TimestampConverter

/**
  * Partial api model for event.
  */
case class ApiPartialEvent(
  description: Option[String],
  start: LocalDateTime,
  end: LocalDateTime,
  notifications: Seq[ApiEvent.NotificationTime]
) {

  /**
    * Converts api model to model.
    *
    * @param id target id
    */
  def toModel(id: Long = 0)(implicit account: User) = Event(
    id,
    description,
    TimestampConverter.toUtc(start, account.timezone),
    TimestampConverter.toUtc(end, account.timezone),
    notifications.map(_.toModel)
  )
}

object ApiPartialEvent {
  implicit val partialEventReads: Reads[ApiPartialEvent] = (
    (__ \ "description").readNullable[String](maxLength(1024)) and
      (__ \ "start").read[LocalDateTime] and
      (__ \ "end").read[LocalDateTime] and
      (__ \ "notifications").read[Seq[ApiEvent.NotificationTime]]
  )(ApiPartialEvent.apply _)
}
