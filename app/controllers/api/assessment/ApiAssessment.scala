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
