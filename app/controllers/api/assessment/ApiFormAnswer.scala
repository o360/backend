package controllers.api.assessment

import controllers.api.assessment.ApiFormAnswer.AnswerStatus
import controllers.api.{ApiNamedEntity, EnumFormat, EnumFormatHelper, Response}
import models.assessment.Answer
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

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
    def toModel = Answer.Element(
      elementId,
      text,
      valuesIds.map(_.toSet),
      comment
    )
  }

  object ElementAnswer {
    implicit val answerElementReads: Reads[ElementAnswer] = (
      (__ \ "elementId").read[Long] and
        (__ \ "text").readNullable[String] and
        (__ \ "valuesIds").readNullable[Seq[Long]] and
        (__ \ "comment").readNullable[String]
    )(ElementAnswer(_, _, _, _))

    def apply(element: Answer.Element): ElementAnswer = ElementAnswer(
      element.elementId,
      element.text,
      element.valuesIds.map(_.toSeq),
      element.comment
    )
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
