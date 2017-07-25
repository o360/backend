package controllers.api.export

import controllers.api.assessment.ApiFormAnswer
import models.assessment.UserAnswer
import play.api.libs.json.Json

/**
  * Api model for answers export.
  */
case class ApiAnswerExport(
  userFrom: String,
  userTo: Option[String],
  answer: ApiFormAnswer,
  isAnonymous: Boolean
)

object ApiAnswerExport {
  implicit val writes = Json.writes[ApiAnswerExport]

  def apply(a: UserAnswer): ApiAnswerExport = ApiAnswerExport(
    a.userFrom,
    a.userTo.map(_.toString),
    ApiFormAnswer(a.answer),
    a.isAnonymous
  )
}
