/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    if (original.status == Event.Status.Completed) {
      Some(AuthorizationError.CompletedEventUpdating)
    } else {
      val rules = getValidationRules(original, draft)
      ValidationRule.validate(rules) match {
        case None => None
        case Some(errorMessage) =>
          val logMessage = s"original: $original; draft: $draft"
          Some(AuthorizationError.FieldUpdate(errorMessage, "event", logMessage))
      }
    }
  }

  /**
    * Returns validation rules for event updating.
    */
  private def getValidationRules(original: Event, draft: Event) = Seq(
    ValidationRule("start", original.start != draft.start) {
      original.status != Event.Status.InProgress && !original.isPreparing
    }
  )

}
