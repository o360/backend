package models.assessment

import models.NamedEntity
import models.form.{Form => FormTemplate}

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
  ) {

    /**
      * Returns answer as text.
      *
      * @param element associated element
      */
    def getText(element: FormTemplate.Element): String = {
      import FormTemplate.ElementKind._
      element.kind match {
        case TextArea | TextField | Checkbox => text.getOrElse("")
        case CheckboxGroup | Radio | Select =>
          def findCaption(id: Long) = element.values.find(_.id == id).map(_.caption)

          valuesIds.map(_.map(findCaption).mkString(", ")).getOrElse("")
      }
    }
  }
}
