package controllers.api.template

import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.template.Template
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import io.scalaland.chimney.dsl._

/**
  * Partial API template model.
  */
case class ApiPartialTemplate(
  name: String,
  subject: String,
  body: String,
  kind: ApiNotificationKind,
  recipient: ApiNotificationRecipient
) {

  def toModel(id: Long = 0): Template =
    this
      .into[Template]
      .withFieldConst(_.id, id)
      .withFieldComputed(_.kind, _.kind.value)
      .withFieldComputed(_.recipient, _.recipient.value)
      .transform
}

object ApiPartialTemplate {

  implicit val templateReads: Reads[ApiPartialTemplate] = (
    (__ \ "name").read[String](maxLength[String](1024)) and
      (__ \ "subject").read[String] and
      (__ \ "body").read[String] and
      (__ \ "kind").read[ApiNotificationKind] and
      (__ \ "recipient").read[ApiNotificationRecipient]
  )(ApiPartialTemplate(_, _, _, _, _))
}
