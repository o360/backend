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

package models.report

import models.assessment.Answer
import models.form.Form
import models.user.User

/**
  * Answers report for single user.
  *
  * @param assessedUser user
  * @param forms        reports by forms
  */
case class Report(
  assessedUser: Option[User],
  forms: Seq[Report.FormReport]
)

object Report {

  /**
    * Report for single form.
    *
    * @param form    form
    * @param answers reports by form elements
    */
  case class FormReport(
    form: Form,
    answers: Seq[FormElementReport]
  )

  /**
    * Report for single form element
    * *
    *
    * @param formElement    form element
    * @param elementAnswers answers
    */
  case class FormElementReport(
    formElement: Form.Element,
    elementAnswers: Seq[FormElementAnswerReport]
  )

  /**
    * Answer of single user.
    *
    * @param fromUser    user
    * @param answer      answer
    * @param isAnonymous is user is hidden
    */
  case class FormElementAnswerReport(
    fromUser: User,
    answer: Answer.Element,
    isAnonymous: Boolean
  )
}
