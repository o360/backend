package controllers.api.template

import controllers.api.Response
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.template.Template
import play.api.libs.json.Json

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
  def apply(t: Template): ApiTemplate = ApiTemplate(
    t.id,
    t.name,
    t.subject,
    t.body,
    ApiNotificationKind(t.kind),
    ApiNotificationRecipient(t.recipient)
  )

  implicit val templateWrites = Json.writes[ApiTemplate]
}
