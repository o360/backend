package models.dao

import javax.inject.{Inject, Singleton}

import models.{ListWithTotal, NamedEntity}
import models.project.Relation
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

/**
  * Component for relation table.
  */
trait ProjectRelationComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  /**
    * Relation db model.
    */
  case class DbRelation(
    id: Long,
    projectId: Long,
    groupFromId: Long,
    groupToId: Option[Long],
    formId: Long,
    kind: Relation.Kind
  ) {

    def toModel(
      projectName: String,
      groupFromName: String,
      groupToName: Option[String],
      formName: String
    ) = Relation(
      id,
      NamedEntity(projectId, projectName),
      NamedEntity(groupFromId, groupFromName),
      groupToId.map(NamedEntity(_, groupToName.getOrElse(""))),
      NamedEntity(formId, formName),
      kind
    )
  }

  object DbRelation {
    def fromModel(r: Relation) = DbRelation(
      r.id,
      r.project.id,
      r.groupFrom.id,
      r.groupTo.map(_.id),
      r.form.id,
      r.kind
    )
  }

  implicit val relationKindColumnType = MappedColumnType.base[Relation.Kind, Byte](
    {
      case Relation.Kind.Classic => 0
      case Relation.Kind.Survey => 1
    }, {
      case 0 => Relation.Kind.Classic
      case 1 => Relation.Kind.Survey
    }
  )

  class RelationTable(tag: Tag) extends Table[DbRelation](tag, "relation") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def projectId = column[Long]("project_id")
    def groupFromId = column[Long]("group_from_id")
    def groupToId = column[Option[Long]]("group_to_id")
    def formId = column[Long]("form_id")
    def kind = column[Relation.Kind]("kind")

    def * = (id, projectId, groupFromId, groupToId, formId, kind) <> ((DbRelation.apply _).tupled, DbRelation.unapply)
  }

  val Relations = TableQuery[RelationTable]
}

/**
  * Project relation DAO.
  */
@Singleton
class ProjectRelationDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with ProjectRelationComponent
  with GroupComponent
  with FormComponent
  with ProjectComponent
  with DaoHelper {

  import driver.api._

  /**
    * Returns list of relations filtered by given criteria.
    */
  def getList(
    optId: Option[Long] = None,
    optProjectId: Option[Long] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Relation]] = {
    val query = Relations
      .applyFilter {
        x =>
          Seq(
            optId.map(x.id === _),
            optProjectId.map(x.projectId === _)
          )
      }
      .join(Groups).on(_.groupFromId === _.id)
      .join(Forms).on { case ((relation, _), form) => relation.formId === form.id }
      .join(Projects).on { case (((relation, _), _), project) => relation.projectId === project.id }
      .joinLeft(Groups).on { case ((((relation, _), _), _), group) => relation.groupToId === group.id }

    runListQuery(query) {
      case ((((relation, _), _), _), _) => {
        case 'id => relation.id
        case 'projectId => relation.projectId
      }
    }.map { case ListWithTotal(total, data) =>
      val result = data.map {
        case ((((relation, groupFrom), form), project), groupTo) =>
          relation.toModel(project.name, groupFrom.name, groupTo.map(_.name), form.name)
      }
      ListWithTotal(total, result)
    }
  }

  /**
    * Finds relation by ID.
    */
  def findById(id: Long): Future[Option[Relation]] = {
    getList(optId = Some(id)).map(_.data.headOption)
  }

  /**
    * Returns true, if relation already exists.
    */
  def exists(relation: Relation): Future[Boolean] = db.run {
    Relations
      .filter { x =>
        x.projectId === relation.project.id &&
          x.groupFromId === relation.groupFrom.id &&
          (relation.groupTo match {
            case None => x.groupToId.isEmpty
            case Some(groupTo) => x.groupToId.fold(false: Rep[Boolean])(_ === groupTo.id)
          }) &&
          x.formId === relation.form.id &&
          x.kind === relation.kind
      }
      .exists
      .result
  }

  /**
    * Creates relation.
    *
    * @return created relation with ID
    */
  def create(model: Relation): Future[Relation] = {
    db.run(Relations.returning(Relations.map(_.id)) += DbRelation.fromModel(model))
      .flatMap(findById(_).map(_.get))
  }

  /**
    * Updates relation.
    *
    * @return updated relation
    */
  def update(model: Relation): Future[Relation] = {
    db.run(Relations.filter(_.id === model.id).update(DbRelation.fromModel(model)))
      .flatMap(_ => findById(model.id).map(_.get))
  }

  /**
    * Removes relation by ID.
    *
    * @return number of rows affected
    */
  def delete(relationId: Long): Future[Int] = db.run {
    Relations.filter(_.id === relationId).delete
  }
}
