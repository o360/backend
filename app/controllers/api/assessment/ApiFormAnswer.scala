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

import controllers.api.assessment.ApiFormAnswer.AnswerStatus
import controllers.api.{ApiNamedEntity, EnumFormat, EnumFormatHelper, Response}
import models.assessment.Answer
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import io.scalaland.chimney.dsl._

/**
  * Api model for form answer.
  */
case class ApiFormAnswer(
  form: ApiNamedEntity,
  answers: Seq[ApiFormAnswer.ElementAnswer],
  isAnonymous: Boolean,
  canSkip: Boolean,
  status: AnswerStatus
) extends Response

object ApiFormAnswer {

  implicit val answerElementWrites = Json.writes[ElementAnswer]
  implicit val answerWrites = Json.writes[ApiFormAnswer]

  def apply(answer: Answer): ApiFormAnswer = ApiFormAnswer(
    ApiNamedEntity(answer.form),
    answer.elements.toSeq.map(ElementAnswer(_)),
    answer.isAnonymous,
    answer.canSkip,
    AnswerStatus(answer.status)
  )

  case class ElementAnswer(
    elementId: Long,
    text: Option[String],
    valuesIds: Option[Seq[Long]],
    comment: Option[String]
  ) {
    def toModel: Answer.Element =
      this
        .into[Answer.Element]
        .withFieldComputed(_.valuesIds, _.valuesIds.map(_.toSet))
        .transform
  }

  object ElementAnswer {
    implicit val answerElementReads: Reads[ElementAnswer] = (
      (__ \ "elementId").read[Long] and
        (__ \ "text").readNullable[String] and
        (__ \ "valuesIds").readNullable[Seq[Long]] and
        (__ \ "comment").readNullable[String]
    )(ElementAnswer(_, _, _, _))

    def apply(element: Answer.Element): ElementAnswer =
      element
        .into[ElementAnswer]
        .withFieldComputed(_.valuesIds, _.valuesIds.map(_.toSeq))
        .transform
  }

  case class AnswerStatus(value: Answer.Status) extends EnumFormat[Answer.Status]
  object AnswerStatus extends EnumFormatHelper[Answer.Status, AnswerStatus]("answer.status") {
    import Answer.Status._

    override protected def mapping: Map[String, Answer.Status] = Map(
      "new" -> New,
      "answered" -> Answered,
      "skipped" -> Skipped
    )
  }
}
