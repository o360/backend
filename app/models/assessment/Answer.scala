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
    * @param isAnonymous is answer anonymous
    */
  case class Form(
    form: NamedEntity,
    answers: Set[Element],
    isAnonymous: Boolean = false
  ) {

    /**
      * Validates form. Returns none in case of success.
      */
    def validateUsing[V](form: FormTemplate)(invalidForm: String => V, requiredAnswerMissed: V): Option[V] = {
      def validateElementAnswer(answerElement: Answer.Element): Option[V] = {
        form.elements.find(_.id == answerElement.elementId) match {
          case Some(formElement) =>
            lazy val answerIsText = answerElement.text.isDefined
            lazy val answerIsValues = answerElement.valuesIds.isDefined
            lazy val needValues = formElement.kind.needValues
            lazy val answerValuesMatchFormValues =
              answerElement.valuesIds.getOrElse(Nil).toSet.subsetOf(formElement.values.map(_.id).toSet)
            lazy val validCheckboxAnswer = formElement.kind != FormTemplate.ElementKind.Checkbox ||
              answerElement.text.exists(t => t == "true" || t == "false")

            if (!needValues && answerIsValues) Some(invalidForm("Text element contains values answer"))
            else if (needValues && !answerValuesMatchFormValues) Some(invalidForm("Values answer contains unknown valueId"))
            else if (!needValues && !answerIsText) Some(invalidForm("Text answer is missed"))
            else if (!validCheckboxAnswer) Some(invalidForm("Wrong checkbox value"))
            else None
          case None => Some(invalidForm("Unknown answer elementId"))
        }
      }

      lazy val answerElementsIds = answers.map(_.elementId)
      lazy val elementAnswersAreDistinct = answerElementsIds.size == answers.size
      lazy val allRequiredElementsAreAnswered = form.elements.filter(_.required).forall(x => answerElementsIds.contains(x.id))
      lazy val maybeElementValidationError = answers.toSeq.map(validateElementAnswer).fold(None) {
        case (err@Some(_), _) => err
        case (None, maybeError) => maybeError
      }

      if (!elementAnswersAreDistinct) Some(invalidForm("Duplicate elementId in answers"))
      else if (!allRequiredElementsAreAnswered) Some(requiredAnswerMissed)
      else if (maybeElementValidationError.nonEmpty) maybeElementValidationError
      else None
    }
  }


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

      def getValuesText = {
        def findCaption(id: Long) = element.values.find(_.id == id).map(_.caption)

        valuesIds
          .map(_
            .map(findCaption)
            .collect { case Some(v) => v }
            .mkString(";\n")
          ).getOrElse("")
      }

      element.kind match {
        case TextArea | TextField | Checkbox => text.getOrElse("")
        case CheckboxGroup | Radio | Select => getValuesText
        case LikeDislike => getValuesText + text.fold("")("\nComment: \"" + _ + "\"")
      }
    }
  }
}
