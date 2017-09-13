package controllers.api.export

import controllers.api.assessment.ApiFormAnswer
import models.assessment.Answer
import play.api.libs.json.Json

/**
  * Api model for answers export.
  */
case class ApiAnswerExport(
  userFrom: Long,
  userTo: Option[Long],
  answer: ApiFormAnswer,
  isAnonymous: Boolean
)

object ApiAnswerExport {
  implicit val writes = Json.writes[ApiAnswerExport]

  def apply(a: Answer): ApiAnswerExport = ApiAnswerExport(
    a.userFromId,
    a.userToId,
    ApiFormAnswer(a),
    a.isAnonymous
  )
}
