package testutils.generator

import models.report.SimpleReport
import org.scalacheck.Arbitrary

/**
  * Simple report generator for scalacheck.
  */
trait SimpleReportGenerator {

  implicit val simpleReportUserArb = Arbitrary {
    for {
      isAnonymous <- Arbitrary.arbitrary[Boolean]
      anonymousId <- Arbitrary.arbitrary[Option[String]]
      id <- Arbitrary.arbitrary[Option[Long]]
    } yield SimpleReport.SimpleReportUser(isAnonymous, anonymousId, id)
  }

  implicit val simpleReportElementArb = Arbitrary {
    for {
      userFrom <- Arbitrary.arbitrary[Option[SimpleReport.SimpleReportUser]]
      formId <- Arbitrary.arbitrary[Long]
      elementId <- Arbitrary.arbitrary[Long]
      text <- Arbitrary.arbitrary[String]
    } yield SimpleReport.SimpleReportElement(userFrom, formId, elementId, text)
  }

  implicit val simpleReportArb = Arbitrary {
    for {
      userToId <- Arbitrary.arbitrary[Option[Long]]
      detailedReports <- Arbitrary.arbitrary[Seq[SimpleReport.SimpleReportElement]]
      aggregatedReports <- Arbitrary.arbitrary[Seq[SimpleReport.SimpleReportElement]]
    } yield SimpleReport(userToId, detailedReports, aggregatedReports)
  }
}
