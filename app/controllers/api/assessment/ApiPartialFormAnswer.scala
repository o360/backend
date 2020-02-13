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

import models.assessment.PartialAnswer
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * Partial API model for assessment form answer.
  */
case class ApiPartialFormAnswer(
  formId: Long,
  answers: Seq[ApiFormAnswer.ElementAnswer],
  isAnonymous: Boolean,
  isSkipped: Boolean
) {
  def toModel: PartialAnswer =
    this
      .into[PartialAnswer]
      .withFieldComputed(_.elements, _.answers.map(_.toModel).toSet)
      .transform
}

object ApiPartialFormAnswer {
  implicit val formReads = Json.reads[ApiPartialFormAnswer]
}
