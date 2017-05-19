package controllers.api.assessment

import models.assessment.Assessment
import models.user.UserShort
import play.api.libs.json.Json

/**
  * Partial API model for assessment answer.
  */
case class ApiPartialAssessment(
  userId: Option[Long],
  form: ApiPartialFormAnswer
) {
  def toModel = Assessment(
    userId.map(UserShort(_)),
    Seq(form.toModel)
  )
}

object ApiPartialAssessment {
  implicit val reads = Json.reads[ApiPartialAssessment]
}
