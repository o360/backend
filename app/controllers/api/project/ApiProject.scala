package controllers.api.project

import controllers.api.Response
import models.project.Project
import play.api.libs.json.Json

/**
  * Project API model.
  */
case class ApiProject(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: Long,
  relations: Seq[ApiProject.Relation]
) extends Response

object ApiProject {

  /**
    * Relation API model.
    */
  case class Relation(
    groupFrom: Long,
    groupTo: Long,
    form: Long
  ) {

    def toModel = Project.Relation(
      groupFrom,
      groupTo,
      form
    )
  }

  object Relation {
    def apply(r: Project.Relation): Relation = Relation(
      r.groupFrom,
      r.groupTo,
      r.form
    )
  }

  implicit val relationFormat = Json.format[Relation]
  implicit val projectWrites = Json.writes[ApiProject]

  def apply(project: Project): ApiProject = ApiProject(
    project.id,
    project.name,
    project.description,
    project.groupAuditor,
    project.relations.map(Relation(_))
  )
}
