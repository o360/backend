package models.project

/**
  * Active project used in event.
  */
case class ActiveProject(
  id: Long,
  eventId: Long,
  name: String,
  description: Option[String],
  formsOnSamePage: Boolean,
  canRevote: Boolean,
  isAnonymous: Boolean,
  machineName: String,
  parentProjectId: Option[Long]
)
