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

package controllers.api.assessment

import controllers.api.Response
import controllers.api.user.ApiShortUser
import models.assessment.Assessment
import models.user.UserShort
import play.api.libs.json.Json

/**
  * Assessment API model.
  */
case class ApiAssessment(
  user: Option[ApiShortUser],
  forms: Seq[ApiFormAnswer]
) extends Response

object ApiAssessment {
  implicit val assessmentWrites = Json.writes[ApiAssessment]

  def apply(assessment: Assessment): ApiAssessment = ApiAssessment(
    assessment.user.map(x => ApiShortUser(UserShort.fromUser(x))),
    assessment.forms.map(ApiFormAnswer(_))
  )
}
