package controllers.api.report

import controllers.api.Response
import models.report.SimpleReport
import play.api.libs.json.Json

/**
  * Report API model.
  */
case class ApiReport(
  userToId: Option[Long],
  detailedReports: Seq[ApiReport.ApiReportElement],
  aggregatedReports: Seq[ApiReport.ApiReportElement]
) extends Response

object ApiReport {

  def apply(rep: SimpleReport): ApiReport = ApiReport(
    rep.userToId,
    rep.detailedReports.map(ApiReportElement(_)),
    rep.aggregatedReports.map(ApiReportElement(_))
  )

  implicit val userWrites = Json.writes[ApiReportUser]
  implicit val elementWrites = Json.writes[ApiReportElement]
  implicit val writes = Json.writes[ApiReport]

  /**
    * Report element API model.
    */
  case class ApiReportElement(
    userFrom: Option[ApiReportUser],
    formId: Long,
    elementId: Long,
    text: String
  )

  object ApiReportElement {
    def apply(el: SimpleReport.SimpleReportElement): ApiReportElement = ApiReportElement(
      el.userFrom.map(ApiReportUser(_)),
      el.formId,
      el.elementId,
      el.text
    )
  }

  /**
    * Report element user API model.
    */
  case class ApiReportUser(
    isAnonymous: Boolean,
    anonymousId: Option[String],
    id: Option[Long]
  )

  object ApiReportUser {
    def apply(u: SimpleReport.SimpleReportUser): ApiReportUser = ApiReportUser(
      u.isAnonymous,
      u.anonymousId,
      u.id
    )
  }
}
