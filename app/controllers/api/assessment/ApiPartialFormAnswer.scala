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
  def toModel =
    this
      .into[PartialAnswer]
      .withFieldComputed(_.elements, _.answers.map(_.toModel).toSet)
      .transform
}

object ApiPartialFormAnswer {
  implicit val formReads = Json.reads[ApiPartialFormAnswer]
}
