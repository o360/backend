package controllers.api.assessment

import controllers.api.Response
import models.assessment.Answer
import play.api.libs.json.Json

/**
  * Api model for form answer.
  */
case class ApiFormAnswer(
  formId: Long,
  answers: Seq[ApiFormAnswer.ElementAnswer]
) extends Response

object ApiFormAnswer {

  implicit val answerElementWrites = Json.writes[ElementAnswer]
  implicit val answerWrites = Json.writes[ApiFormAnswer]

  def apply(answer: Answer.Form): ApiFormAnswer = ApiFormAnswer(
    answer.formId,
    answer.answers.toSeq.map(ElementAnswer(_))
  )

  case class ElementAnswer(
    elementId: Long,
    text: Option[String],
    valuesIds: Option[Seq[Long]]
  )

  object ElementAnswer {
    def apply(element: Answer.Element): ElementAnswer = ElementAnswer(
      element.elementId,
      element.text,
      element.valuesIds
    )
  }
}
