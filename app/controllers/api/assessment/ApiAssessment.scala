package controllers.api.assessment

import controllers.api.Response
import controllers.api.user.ApiUser
import models.assessment.Assessment
import models.user.UserShort
import play.api.libs.json.Json

/**
  * Assessment API model.
  */
case class ApiAssessment(
  user: Option[ApiAssessment.User],
  formIds: Seq[Long]
) extends Response

object ApiAssessment {
  implicit val userWrites = Json.writes[User]
  implicit val assessmentWrites = Json.writes[ApiAssessment]

  def apply(assessment: Assessment): ApiAssessment = ApiAssessment(
    assessment.user.map(ApiAssessment.User(_)),
    assessment.formIds
  )

  case class User(
    id: Long,
    name: String,
    gender: ApiUser.ApiGender
  )

  object User {

    def apply(user: UserShort): User = User(
      user.id,
      user.name,
      ApiUser.ApiGender(user.gender)
    )
  }
}
