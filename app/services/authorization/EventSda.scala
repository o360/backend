package services.authorization

import models.event.Event
import utils.errors.AuthorizationError

/**
  * SDA rules for event model.
  */
object EventSda {

  /**
    * Returns some error if update forbidden.
    */
  def canUpdate(original: Event, draft: Event): Option[AuthorizationError] = {
      val rules = getValidationRules(original, draft)
      ValidationRule.validate(rules) match {
        case None => None
        case Some(errorMessage) =>
          val logMessage = s"original: $original; draft: $draft"
          Some(AuthorizationError.FieldUpdate(errorMessage, "event", logMessage))
      }

  }

  /**
    * Returns validation rules for event updating.
    */
  private def getValidationRules(original: Event, draft: Event) = Seq(
    ValidationRule("start", original.start != draft.start) {
      original.status != Event.Status.InProgress
    }
  )

}
