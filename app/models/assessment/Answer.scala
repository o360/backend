package models.assessment

import models.NamedEntity

/**
  * Answers.
  */
object Answer {
  /**
    * Answer for form.
    *
    * @param form    form ID with name
    * @param answers answers for elements.
    */
  case class Form(
    form: NamedEntity,
    answers: Set[Element]
  )


  /**
    * Answer for form element.
    *
    * @param elementId ID of form element
    * @param text      text answer
    * @param valuesIds predefined answers IDs list
    */
  case class Element(
    elementId: Long,
    text: Option[String],
    valuesIds: Option[Seq[Long]]
  )

}
