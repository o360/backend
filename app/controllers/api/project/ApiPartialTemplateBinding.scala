package controllers.api.project

import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.NamedEntity
import models.project.TemplateBinding
import play.api.libs.json.Json

/**
  * Partial API model for email template binding.
  */
case class ApiPartialTemplateBinding(
  templateId: Long,
  kind: ApiNotificationKind,
  recipient: ApiNotificationRecipient
) {
  def toModel = TemplateBinding(
    NamedEntity(templateId),
    kind.value,
    recipient.value
  )
}

object ApiPartialTemplateBinding {
  implicit val reads = Json.reads[ApiPartialTemplateBinding]
}
