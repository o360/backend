package controllers.api.export

import controllers.api.assessment.ApiFormAnswer
import models.assessment.Answer
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

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

  def apply(a: Answer): ApiAnswerExport =
    a.into[ApiAnswerExport]
      .withFieldRenamed(_.userFromId, _.userFrom)
      .withFieldRenamed(_.userToId, _.userTo)
      .withFieldComputed(_.answer, ApiFormAnswer(_))
      .transform
}
