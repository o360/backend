package controllers.api.project

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
  groupAuditor: Long,
  relations: Seq[ApiProject.Relation]
) {

  def toModel(id: Long = 0) = Project(
    id,
    name,
    description,
    groupAuditor,
    relations.map(_.toModel)
  )
}

object ApiPartialProject {

  implicit val reads: Reads[ApiPartialProject] = (
    (__ \ "name").read[String](maxLength[String](1024)) and
      (__ \ "description").readNullable[String](maxLength[String](1024)) and
      (__ \ "groupAuditor").read[Long] and
      (__ \ "relations").read[Seq[ApiProject.Relation]]
    ) (ApiPartialProject(_, _, _, _))

  def apply(project: Project): ApiPartialProject = ApiPartialProject(
    project.name,
    project.description,
    project.groupAuditor,
    project.relations.map(ApiProject.Relation(_))
  )
}
