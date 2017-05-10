package models.project

import models.NamedEntity

/**
  * Project model.
  *
  * @param id           DB iD
  * @param name         project name
  * @param description  description
  * @param groupAuditor group-auditor
  * @param templates    project-wide email templates
  */
case class Project(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: NamedEntity,
  templates: Seq[TemplateBinding]
)
