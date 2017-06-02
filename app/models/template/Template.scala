package models.template

import models.notification.Notification

/**
  * Email template model.
  *
  * @param id        DB ID
  * @param name      name
  * @param subject   mail subject
  * @param body      HTML body
  * @param kind      kind
  * @param recipient recipient
  */
case class Template(
  id: Long,
  name: String,
  subject: String,
  body: String,
  kind: Notification.Kind,
  recipient: Notification.Recipient
)

object Template {
  val nameSingular = "template"
}
