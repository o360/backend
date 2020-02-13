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

import models.form.Form
import models.user.User

/**
  * Aggregated report for single user.
  *
  * @param assessedUser user
  * @param forms        aggregated reports by forms
  */
case class AggregatedReport(
  assessedUser: Option[User],
  forms: Seq[AggregatedReport.FormAnswer]
)

object AggregatedReport {

  /**
    * Aggregated report for single form.
    *
    * @param form    form
    * @param answers reports for form elements
    */
  case class FormAnswer(
    form: Form,
    answers: Seq[FormElementAnswer]
  )

  /**
    * Aggregated report for single form element.
    *
    * @param element           form element
    * @param aggregationResult aggregation result
    */
  case class FormElementAnswer(
    element: Form.Element,
    aggregationResult: String
  )

}
