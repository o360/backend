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

package models.assessment

import models.NamedEntity
import models.form.{Form => FormTemplate}
import models.form.element._

/**
  * Assessment answer.
  */
case class Answer(
  activeProjectId: Long,
  userFromId: Long,
  userToId: Option[Long],
  form: NamedEntity,
  canSkip: Boolean,
  status: Answer.Status = Answer.Status.New,
  isAnonymous: Boolean = false,
  elements: Set[Answer.Element] = Set()
) {

  def getUniqueComponent = (activeProjectId, userFromId, userToId, form.id)

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
          lazy val validCheckboxAnswer = formElement.kind != Checkbox ||
            answerElement.text.exists(t => t == "true" || t == "false")

          if (!needValues && answerIsValues) Some(invalidForm("Text element contains values answer"))
          else if (needValues && !answerValuesMatchFormValues)
            Some(invalidForm("Values answer contains unknown valueId"))
          else if (!needValues && !answerIsText) Some(invalidForm("Text answer is missed"))
          else if (!validCheckboxAnswer) Some(invalidForm("Wrong checkbox value"))
          else None
        case None => Some(invalidForm("Unknown answer elementId"))
      }
    }

    lazy val answerElementsIds = elements.map(_.elementId)
    lazy val elementAnswersAreDistinct = answerElementsIds.size == elements.size
    lazy val allRequiredElementsAreAnswered =
      form.elements.filter(_.required).forall(x => answerElementsIds.contains(x.id))
    lazy val maybeElementValidationError = elements.toSeq.map(validateElementAnswer).fold(None) {
      case (err @ Some(_), _) => err
      case (None, maybeError) => maybeError
    }

    if (!elementAnswersAreDistinct) Some(invalidForm("Duplicate elementId in answers"))
    else if (!allRequiredElementsAreAnswered) Some(requiredAnswerMissed)
    else if (maybeElementValidationError.nonEmpty) maybeElementValidationError
    else None
  }
}

/**
  * Answers.
  */
object Answer {

  /**
    * Answer for form element.
    *
    * @param elementId ID of form element
    * @param text      text answer
    * @param valuesIds predefined answers IDs list
    * @param comment   optional comment
    */
  case class Element(
    elementId: Long,
    text: Option[String],
    valuesIds: Option[Set[Long]],
    comment: Option[String]
  ) {

    /**
      * Returns answer as text.
      *
      * @param element associated element
      */
    def getText(element: FormTemplate.Element): String = {

      def getValuesText = {
        def findCaption(id: Long) = element.values.find(_.id == id).map(_.caption)

        valuesIds
          .map(
            _.map(findCaption)
              .collect { case Some(v) => v }
              .mkString(";\n")
          )
          .getOrElse("")
      }

      val basePart = element.kind match {
        case TextArea | TextField | Checkbox => text.getOrElse("")
        case CheckboxGroup | Radio | Select  => getValuesText
        case LikeDislike                     => getValuesText + text.map(t => s" ($t)").getOrElse("")
      }

      comment.fold(basePart)(c => s"$basePart ($c)")
    }
  }

  /**
    * Answer state.
    */
  sealed trait Status
  object Status {

    /**
      * Automatically created answer.
      */
    case object New extends Status

    /**
      * Answered answer.
      */
    case object Answered extends Status

    /**
      * Skipped answer.
      */
    case object Skipped extends Status
  }
}
