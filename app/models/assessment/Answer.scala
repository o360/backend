package models.assessment

/**
  * Answers.
  */
object Answer {
  /**
    * Answer for form.
    *
    * @param formId  ID of form
    * @param answers answers for elements.
    */
  case class Form(
    formId: Long,
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
