package controllers.api.assessment

import models.assessment.PartialAssessment
import play.api.libs.json.Json

/**
  * Partial API model for assessment answer.
  */
case class ApiPartialAssessment(
  userId: Option[Long],
  form: ApiPartialFormAnswer
) {
  def toModel = PartialAssessment(
    userId,
    Seq(form.toModel)
  )
}

object ApiPartialAssessment {
  implicit val reads = Json.reads[ApiPartialAssessment]
}
