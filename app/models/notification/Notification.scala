package models.notification

/**
  * Notification kind.
  */
sealed trait NotificationKind

/**
  * Notification for soon event starting.
  */
case object PreBegin extends NotificationKind

/**
  * Notification for event start.
  */
case object Begin extends NotificationKind

/**
  * Notification for soon event ending.
  */
case object PreEnd extends NotificationKind

/**
  * Notification for event ending.
  */
case object End extends NotificationKind

/**
  * Kind of notification recipient.
  */
sealed trait NotificationRecipient

/**
  * Notification for project auditor.
  */
case object Auditor extends NotificationRecipient

/**
  * Notification for respondent.
  */
case object Respondent extends NotificationRecipient
