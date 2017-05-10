package controllers.api.notification

import controllers.api.{EnumFormat, EnumFormatHelper}
import models.notification.Notification

/**
  * Kind of notification recipient.
  */
case class ApiNotificationRecipient(value: Notification.Recipient) extends EnumFormat[Notification.Recipient]
object ApiNotificationRecipient
  extends EnumFormatHelper[Notification.Recipient, ApiNotificationRecipient]("notification recipient") {

  import Notification.Recipient._

  override protected def mapping: Map[String, Notification.Recipient] = Map(
    "respondent" -> Respondent,
    "auditor" -> Auditor
  )
}
