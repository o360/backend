package models.project

import models.NamedEntity

/**
  * Project model.
  *
  * @param id              DB iD
  * @param name            project name
  * @param description     description
  * @param groupAuditor    group-auditor
  * @param templates       project-wide email templates
  * @param formsOnSamePage is all forms displayed on same assessment page
  * @param canRevote       if true user can revote
  */
case class Project(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: NamedEntity,
  templates: Seq[TemplateBinding],
  formsOnSamePage: Boolean,
  canRevote: Boolean
) {
  def toNamedEntity = NamedEntity(id, name)
}

object Project {
  val namePlural = "projects"
  val nameSingular = "project"
}
