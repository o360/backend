package models.report

import models.form.Form
import models.user.User

/**
  * Aggregated report for single user.
  *
  * @param assessedUser user
  * @param forms        aggregated reports by forms
  */
case class AggregatedReport(
  assessedUser: Option[User],
  forms: Seq[AggregatedReport.FormAnswer]
)

object AggregatedReport {

  /**
    * Aggregated report for single form.
    *
    * @param form    form
    * @param answers reports for form elements
    */
  case class FormAnswer(
    form: Form,
    answers: Seq[FormElementAnswer]
  )

  /**
    * Aggregated report for single form element.
    *
    * @param element           form element
    * @param aggregationResult aggregation result
    */
  case class FormElementAnswer(
    element: Form.Element,
    aggregationResult: String
  )

}
