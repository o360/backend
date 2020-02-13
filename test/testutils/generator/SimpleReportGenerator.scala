/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
