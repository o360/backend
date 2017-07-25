package models.assessment

/**
  * User answers.
  */
case class UserAnswer(
  userFrom: String,
  userFromId: Long,
  userTo: Option[Long],
  answer: Answer.Form
) {
  val isAnonymous: Boolean = answer.isAnonymous
}
