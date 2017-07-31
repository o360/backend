package models.assessment

/**
  * User answers.
  */
case class UserAnswer(
  userFrom: String,
  userFromId: Long,
  userTo: Option[Long],
  answer: Answer.Form,
  projectMachineName: String,
  formMachineName: String
) {
  val isAnonymous: Boolean = answer.isAnonymous
}
