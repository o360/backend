package models.notification

object Notification {

  /**
    * Notification kind.
    */
  sealed trait Kind
  object Kind {
    /**
      * Notification for soon event starting.
      */
    case object PreBegin extends Kind
    /**
      * Notification for event start.
      */
    case object Begin extends Kind
    /**
      * Notification for soon event ending.
      */
    case object PreEnd extends Kind
    /**
      * Notification for event ending.
      */
    case object End extends Kind
  }

  /**
    * Kind of notification recipient.
    */
  sealed trait Recipient
  object Recipient {
    /**
      * Notification for project auditor.
      */
    case object Auditor extends Recipient
    /**
      * Notification for respondent.
      */
    case object Respondent extends Recipient
  }
}

