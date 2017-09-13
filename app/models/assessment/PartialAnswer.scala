package models.assessment

/**
  * Partial answer.
  */
case class PartialAnswer(
  formId: Long,
  isAnonymous: Boolean,
  elements: Set[Answer.Element]
)
