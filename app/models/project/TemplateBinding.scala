package models.project

import models.NamedEntity
import models.notification.Notification

/**
  * Email template binding.
  * Binds notification kind and notification recipient to template.
  */
case class TemplateBinding(
  template: NamedEntity,
  kind: Notification.Kind,
  recipient: Notification.Recipient
)
