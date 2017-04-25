package controllers.api.project

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
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
    groupTo: Option[Long],
    form: Long,
    kind: RelationKind
  ) {

    def toModel = Project.Relation(
      groupFrom,
      groupTo,
      form,
      kind.value
    )
  }

  object Relation {
    def apply(r: Project.Relation): Relation = Relation(
      r.groupFrom,
      r.groupTo,
      r.form,
      RelationKind(r.kind)
    )
  }

  case class RelationKind(value: Project.RelationKind) extends EnumFormat[Project.RelationKind]
  object RelationKind extends EnumFormatHelper[Project.RelationKind, RelationKind]("relation kind") {

    override protected def mapping: Map[String, Project.RelationKind] = Map(
      "classic" -> Project.RelationKind.Classic,
      "survey" -> Project.RelationKind.Survey
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
