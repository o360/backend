package controllers.api.project

import models.NamedEntity
import models.project.Project
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * Project partial API model.
  */
case class ApiPartialProject(
  name: String,
  description: Option[String],
  groupAuditorId: Long,
  templates: Seq[ApiPartialTemplateBinding],
  formsOnSamePage: Boolean,
  canRevote: Boolean
) {

  def toModel(id: Long = 0) = Project(
    id,
    name,
    description,
    NamedEntity(groupAuditorId),
    templates.map(_.toModel),
    formsOnSamePage,
    canRevote
  )
}

object ApiPartialProject {

  implicit val reads: Reads[ApiPartialProject] = (
    (__ \ "name").read[String](maxLength[String](1024)) and
      (__ \ "description").readNullable[String](maxLength[String](1024)) and
      (__ \ "groupAuditorId").read[Long] and
      (__ \ "templates").read[Seq[ApiPartialTemplateBinding]] and
      (__ \ "formsOnSamePage").read[Boolean] and
      (__ \ "canRevote").read[Boolean]
    ) (ApiPartialProject(_, _, _, _, _, _))
}
