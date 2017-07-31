package controllers.api.project

import java.util.UUID

import models.NamedEntity
import models.project.Project
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.MachineNameGenerator

/**
  * Project partial API model.
  */
case class ApiPartialProject(
  name: String,
  description: Option[String],
  groupAuditorId: Long,
  templates: Seq[ApiPartialTemplateBinding],
  formsOnSamePage: Boolean,
  canRevote: Boolean,
  isAnonymous: Boolean,
  machineName: Option[String]
) {

  def toModel(id: Long = 0) = Project(
    id,
    name,
    description,
    NamedEntity(groupAuditorId),
    templates.map(_.toModel),
    formsOnSamePage,
    canRevote,
    isAnonymous,
    hasInProgressEvents = false,
    machineName.getOrElse(MachineNameGenerator.generate)
  )
}

object ApiPartialProject {

  implicit val reads: Reads[ApiPartialProject] = (
    (__ \ "name").read[String](maxLength[String](1024)) and
      (__ \ "description").readNullable[String](maxLength[String](1024)) and
      (__ \ "groupAuditorId").read[Long] and
      (__ \ "templates").read[Seq[ApiPartialTemplateBinding]] and
      (__ \ "formsOnSamePage").read[Boolean] and
      (__ \ "canRevote").read[Boolean] and
      (__ \ "isAnonymous").read[Boolean] and
      (__ \ "machineName").readNullable[String]
    ) (ApiPartialProject(_, _, _, _, _, _, _, _))
}
