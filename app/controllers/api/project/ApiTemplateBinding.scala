package controllers.api.project

import controllers.api.{ApiNamedEntity, Response}
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.project.TemplateBinding
import play.api.libs.json.Json

/**
  * API model for email template binding.
  */
case class ApiTemplateBinding(
  template: ApiNamedEntity,
  kind: ApiNotificationKind,
  recipient: ApiNotificationRecipient
) extends Response

object ApiTemplateBinding {
  implicit val writes = Json.writes[ApiTemplateBinding]

  def apply(tb: TemplateBinding): ApiTemplateBinding = ApiTemplateBinding(
    ApiNamedEntity(tb.template),
    ApiNotificationKind(tb.kind),
    ApiNotificationRecipient(tb.recipient)
  )
}
