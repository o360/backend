package models.dao

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
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

    def toModel = Relation(
      id,
      projectId,
      groupFromId,
      groupToId,
      formId,
      kind
    )
  }

  object DbRelation {
    def fromModel(r: Relation) = DbRelation(
      r.id,
      r.projectId,
      r.groupFrom,
      r.groupTo,
      r.form,
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

    runListQuery(query) {
      relation => {
        case 'id => relation.id
        case 'projectId => relation.projectId
      }
    }.map { case ListWithTotal(total, data) =>
      ListWithTotal(total, data.map(_.toModel))
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
        x.projectId === relation.projectId &&
          x.groupFromId === relation.groupFrom &&
          (relation.groupTo match {
            case None => x.groupToId.isEmpty
            case Some(groupToId) => x.groupToId.fold(false: Rep[Boolean])(_ === groupToId)
          }) &&
          x.formId === relation.form &&
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
      .map(id => model.copy(id = id))
  }

  /**
    * Updates relation.
    *
    * @return updated relation
    */
  def update(model: Relation): Future[Relation] = {
    db.run(Relations.filter(_.id === model.id).update(DbRelation.fromModel(model)))
      .map(_ => model)
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
