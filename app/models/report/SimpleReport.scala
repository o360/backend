package models.report

/**
  *
  */
case class SimpleReport(
  userToId: Option[Long],
  detailedReports: Seq[SimpleReport.SimpleReportElement],
  aggregatedReports: Seq[SimpleReport.SimpleReportElement]
)

object SimpleReport {

  case class SimpleReportElement(
    userFrom: Option[SimpleReportUser],
    formId: Long,
    elementId: Long,
    text: String
  )

  /**
    * Report element user API model.
    */
  case class SimpleReportUser(
    isAnonymous: Boolean,
    anonymousId: Option[String],
    id: Option[Long]
  )

}
