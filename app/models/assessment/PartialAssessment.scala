package models.assessment

/**
  * Partial assessment.
  */
case class PartialAssessment(
  userToId: Option[Long],
  answers: Seq[PartialAnswer]
)
