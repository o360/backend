package models.dao

import javax.inject.{Inject, Singleton}

import models.event.Event
import models.{ListWithTotal, NamedEntity}
import models.project.{Relation, TemplateBinding}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import utils.implicits.FutureLifting._


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
    kind: Relation.Kind,
    canSelfVote: Boolean
  ) {

    def toModel(
      projectName: String,
      groupFromName: String,
      groupToName: Option[String],
      formName: String,
      templates: Seq[TemplateBinding],
      isEventsExists: Boolean
    ) = Relation(
      id,
      NamedEntity(projectId, projectName),
      NamedEntity(groupFromId, groupFromName),
      groupToId.map(NamedEntity(_, groupToName.getOrElse(""))),
      NamedEntity(formId, formName),
      kind,
      templates,
      isEventsExists,
      canSelfVote
    )
  }

  object DbRelation {
    def fromModel(r: Relation) = DbRelation(
      r.id,
      r.project.id,
      r.groupFrom.id,
      r.groupTo.map(_.id),
      r.form.id,
      r.kind,
      r.canSelfVote
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
    def canSelfVote = column[Boolean]("can_self_vote")

    def * = (id, projectId, groupFromId, groupToId, formId, kind, canSelfVote) <> ((DbRelation.apply _).tupled, DbRelation.unapply)
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
  with EventProjectComponent
  with EventComponent
  with TemplateBindingComponent
  with TemplateComponent
  with DaoHelper {

  import driver.api._

  /**
    * Returns list of relations filtered by given criteria.
    */
  def getList(
    optId: Option[Long] = None,
    optProjectId: Option[Long] = None,
    optKind: Option[Relation.Kind] = None,
    optFormId: Option[Long] = None,
    optGroupFromId: Option[Long] = None,
    optGroupToId: Option[Long] = None,
    optEmailTemplateId: Option[Long] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Relation]] = {

    def sortMapping(relation: RelationTable): PartialFunction[Symbol, Rep[_]] = {
      case 'id => relation.id
      case 'projectId => relation.projectId
    }

    def emailTemplateFilter(relation: RelationTable) = optEmailTemplateId.map { emailTemplateId =>
      relation.id in RelationTemplates.filter(_.templateId === emailTemplateId).map(_.relationId)
    }

    val baseQuery = Relations
      .applyFilter {
        x =>
          Seq(
            optId.map(x.id === _),
            optProjectId.map(x.projectId === _),
            optKind.map(x.kind === _),
            optFormId.map(x.formId === _),
            optGroupFromId.map(x.groupFromId === _),
            optGroupToId.map(groupTo => x.groupToId.fold(false: Rep[Boolean])(_ === groupTo)),
            emailTemplateFilter(x)
          )
      }

    val countQuery = baseQuery.length

    val templatesQuery = RelationTemplates.join(Templates).on(_.templateId === _.id)

    val resultQuery = baseQuery
      .join(Groups).on(_.groupFromId === _.id)
      .join(Forms).on { case ((relation, _), form) => relation.formId === form.id }
      .join(Projects).on { case (((relation, _), _), project) => relation.projectId === project.id }
      .joinLeft(Groups).on { case ((((relation, _), _), _), group) => relation.groupToId === group.id }
      .applySorting(meta.sorting) { case ((((relation, _), _), _), _) => sortMapping(relation) }
      .applyPagination(meta.pagination)
      .joinLeft(templatesQuery).on { case (((((relation, _), _), _), _), (template, _)) =>
        relation.id === template.relationId
      }
        .map { case (((((relation, groupFrom), form), project), groupTo), templateOpt) =>
          val eventsIds = EventProjects.filter(_.projectId === project.id).map(_.eventId)
          val isEventsExists = Events
            .filter(event => event.id.in(eventsIds) && statusFilter(event, Event.Status.InProgress))
            .exists
          ((relation, groupFrom, form, project, groupTo, isEventsExists), templateOpt)
        }
      .applySorting(meta.sorting) { case ((relation, _, _, _, _, _), _) => sortMapping(relation) } // sort one page (order not preserved after join)

    for {
      count <- db.run(countQuery.result)
      flatResult <- if (count > 0) db.run(resultQuery.result) else Nil.toFuture
    } yield {
      val data = flatResult
        .groupByWithOrder { case ((relation, groupFrom, form, project, groupTo, isEventsExists), _) =>
          (relation, groupFrom, form, project, groupTo, isEventsExists)
        }
        .map { case ((relation, groupFrom, form, project, groupTo, isEventsExists), flatTemplates) =>
          val templates = flatTemplates
            .collect { case (_, Some((templateBinding, template))) => templateBinding.toModel(template.name) }

          relation.toModel(project.name, groupFrom.name, groupTo.map(_.name), form.name, templates, isEventsExists)
        }
      ListWithTotal(count, data)
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
  def exists(relation: Relation): Future[Boolean] = {
    def groupToFilter(x: RelationTable) = relation.groupTo match {
      case None => x.groupToId.isEmpty
      case Some(groupTo) => x.groupToId.fold(false: Rep[Boolean])(_ === groupTo.id)
    }

    db.run {
      Relations
        .filter { x =>
          x.projectId === relation.project.id &&
            x.groupFromId === relation.groupFrom.id &&
            groupToFilter(x) &&
            x.formId === relation.form.id &&
            x.kind === relation.kind
        }
        .exists
        .result
    }
  }

  /**
    * Creates relation.
    *
    * @return created relation with ID
    */
  def create(model: Relation): Future[Relation] = {
    db.run {
      (for {
        relationId <- Relations.returning(Relations.map(_.id)) += DbRelation.fromModel(model)
        _ <- DBIO.seq(RelationTemplates ++= model.templates.map(DbTemplateBinding.fromModel(_, relationId)))
      } yield relationId).transactionally
    }.flatMap(findById(_).map(_.getOrElse(throw new NoSuchElementException("relation not found"))))
  }

  /**
    * Updates relation.
    *
    * @return updated relation
    */
  def update(model: Relation): Future[Relation] = {
    db.run {
      (for {
        _ <- Relations.filter(_.id === model.id).update(DbRelation.fromModel(model))
        _ <- RelationTemplates.filter(_.relationId === model.id).delete
        _ <- DBIO.seq(RelationTemplates ++= model.templates.map(DbTemplateBinding.fromModel(_, model.id)))
      } yield ()).transactionally
    }.flatMap(_ => findById(model.id).map(_.getOrElse(throw new NoSuchElementException("relation not found"))))
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
