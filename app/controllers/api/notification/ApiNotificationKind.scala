package controllers.api.notification

import controllers.api.{EnumFormat, EnumFormatHelper}
import models.notification._

/**
  * Kind of notification.
  */
case class ApiNotificationKind(value: NotificationKind) extends EnumFormat[NotificationKind]
object ApiNotificationKind extends EnumFormatHelper[NotificationKind, ApiNotificationKind]("notification kind") {

  override protected def mapping: Map[String, NotificationKind] = Map(
    "preBegin" -> PreBegin,
    "begin" -> Begin,
    "preEnd" -> PreEnd,
    "end" -> End
  )
}
