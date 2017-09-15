package controllers.api.template

import controllers.api.Response
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.template.Template
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * API template model.
  */
case class ApiTemplate(
  id: Long,
  name: String,
  subject: String,
  body: String,
  kind: ApiNotificationKind,
  recipient: ApiNotificationRecipient
) extends Response

object ApiTemplate {
  def apply(t: Template): ApiTemplate =
    t.into[ApiTemplate]
      .withFieldComputed(_.kind, x => ApiNotificationKind(x.kind))
      .withFieldComputed(_.recipient, x => ApiNotificationRecipient(x.recipient))
      .transform

  implicit val templateWrites = Json.writes[ApiTemplate]
}
