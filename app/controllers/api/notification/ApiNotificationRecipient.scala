package controllers.api.notification

import controllers.api.{EnumFormat, EnumFormatHelper}
import models.notification._

/**
  * Kind of notification recipient.
  */
case class ApiNotificationRecipient(value: NotificationRecipient) extends EnumFormat[NotificationRecipient]
object ApiNotificationRecipient
  extends EnumFormatHelper[NotificationRecipient, ApiNotificationRecipient]("notification recipient") {

  override protected def mapping: Map[String, NotificationRecipient] = Map(
    "respondent" -> Respondent,
    "auditor" -> Auditor
  )
}
