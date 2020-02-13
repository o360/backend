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
