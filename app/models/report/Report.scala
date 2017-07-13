package models.report

import models.assessment.Answer
import models.form.Form
import models.user.User

/**
  * Answers report for single user.
  *
  * @param assessedUser user
  * @param forms        reports by forms
  */
case class Report(
  assessedUser: Option[User],
  forms: Seq[Report.FormReport]
)

object Report {
  /**
    * Report for single form.
    *
    * @param form    form
    * @param answers reports by form elements
    */
  case class FormReport(
    form: Form,
    answers: Seq[FormElementReport]
  )

  /**
    * Report for single form element
    * *
    *
    * @param formElement    form element
    * @param elementAnswers answers
    */
  case class FormElementReport(
    formElement: Form.Element,
    elementAnswers: Seq[FormElementAnswerReport]
  )

  /**
    * Answer of single user.
    *
    * @param fromUser    user
    * @param answer      answer
    * @param isAnonymous is user is hidden
    */
  case class FormElementAnswerReport(
    fromUser: User,
    answer: Answer.Element,
    isAnonymous: Boolean
  )
}
