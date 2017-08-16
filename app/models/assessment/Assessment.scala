package models.assessment

import models.NamedEntity
import models.user.{User, UserShort}

/**
  * Assessment object model.
  *
  * @param user  assessed user
  * @param forms forms ids with answers
  */
case class Assessment(
  user: Option[UserShort],
  forms: Seq[Answer.Form]
)

object Assessment {

  /**
    * Creates new assessment object from formsWithAnswers list.
    *
    * @param formsWithAnswers forms IDs paired with answers.
    * @param user             assessed user
    */
  def apply(formsWithAnswers: Seq[(NamedEntity, Option[Answer.Form])], user: Option[User] = None): Assessment = {
    val forms = formsWithAnswers.map {
      case (form, answer) =>
        answer.getOrElse(Answer.Form(form, Set(), answer.fold(false)(_.isAnonymous)))
    }
    Assessment(user.map(UserShort.fromUser), forms)
  }
}
