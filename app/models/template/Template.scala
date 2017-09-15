package models.template

import models.notification._

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
  kind: NotificationKind,
  recipient: NotificationRecipient
)

object Template {
  val nameSingular = "template"
}
