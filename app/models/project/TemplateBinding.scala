package models.project

import models.NamedEntity
import models.notification._

/**
  * Email template binding.
  * Binds notification kind and notification recipient to template.
  */
case class TemplateBinding(
  template: NamedEntity,
  kind: NotificationKind,
  recipient: NotificationRecipient
)
