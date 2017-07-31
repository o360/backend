package models.project

import models.NamedEntity

/**
  * Project model.
  *
  * @param id                  DB iD
  * @param name                project name
  * @param description         description
  * @param groupAuditor        group-auditor
  * @param templates           project-wide email templates
  * @param formsOnSamePage     is all forms displayed on same assessment page
  * @param canRevote           if true user can revote
  * @param isAnonymous         is answers in project anonymous by default
  * @param hasInProgressEvents is project has in progress events
  * @param machineName         machine name
  */
case class Project(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: NamedEntity,
  templates: Seq[TemplateBinding],
  formsOnSamePage: Boolean,
  canRevote: Boolean,
  isAnonymous: Boolean,
  hasInProgressEvents: Boolean,
  machineName: String
) {
  def toNamedEntity = NamedEntity(id, name)
}

object Project {
  val namePlural = "projects"
  val nameSingular = "project"
}
