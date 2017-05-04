package controllers.api.notification

import controllers.api.{EnumFormat, EnumFormatHelper}
import models.notification.Notification

/**
  * Kind of notification.
  */
case class ApiNotificationKind(value: Notification.Kind) extends EnumFormat[Notification.Kind]
object ApiNotificationKind extends EnumFormatHelper[Notification.Kind, ApiNotificationKind]("notification kind") {

  import Notification.Kind._

  override protected def mapping: Map[String, Notification.Kind] = Map(
    "preBegin" -> PreBegin,
    "begin" -> Begin,
    "preEnd" -> PreEnd,
    "end" -> End
  )
}
