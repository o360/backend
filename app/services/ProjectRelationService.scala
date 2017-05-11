package services

import javax.inject.{Inject, Singleton}

import models.dao.{EventDao, ProjectRelationDao}
import models.event.Event
import models.project.Relation
import models.user.User
import utils.errors._
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

/**
  * Project relation service.
  */
@Singleton
class ProjectRelationService @Inject()(
  protected val projectRelationDao: ProjectRelationDao,
  protected val eventDao: EventDao
) extends ServiceResults[Relation] {

  /**
    * Returns relation by ID
    */
  def getById(id: Long)(implicit account: User): SingleResult = {
    projectRelationDao.findById(id)
      .liftRight {
        NotFoundError.ProjectRelation(id)
      }
  }

  /**
    * Returns relations list.
    */
  def getList(projectId: Option[Long])(implicit account: User, meta: ListMeta): ListResult = {
    projectRelationDao.getList(optId = None, optProjectId = projectId).lift
  }

  /**
    * Creates new relation.
    *
    * @param relation relation model
    */
  def create(relation: Relation)(implicit account: User): SingleResult = {
    for {
      validated <- validateRelation(relation)

      _ <- ensure(!projectRelationDao.exists(validated)) {
        BadRequestError.Relation.DuplicateRelation
      }

      created <- projectRelationDao.create(validated).lift(ExceptionHandler.sql)
    } yield created
  }

  /**
    * Updates relation.
    *
    * @param draft relation draft
    */
  def update(draft: Relation)(implicit account: User): SingleResult = {
    def isSameRelations(left: Relation, right: Relation) = {
      left.copy(id = 0, templates = Nil) == right.copy(id = 0, templates = Nil)
    }

    for {
      original <- getById(draft.id)

      _ <- ensure(original.project.id == draft.project.id) {
        BadRequestError.Relation.ProjectIdIsImmutable
      }

      validated <- validateRelation(draft)

      duplicateExists <- projectRelationDao.exists(validated).lift

      _ <- ensure(!duplicateExists || isSameRelations(original, validated)) {
        BadRequestError.Relation.DuplicateRelation
      }

      updated <- projectRelationDao.update(validated).lift(ExceptionHandler.sql)
    } yield updated
  }

  /**
    * Removes relation.
    *
    * @param id relation ID
    */
  def delete(id: Long)(implicit account: User): UnitResult = {
    for {
      _ <- getById(id)

      _ <- projectRelationDao.delete(id).lift(ExceptionHandler.sql)
    } yield ()
  }

  /**
    * Validate relation and returns either new relation or error.
    */
  private def validateRelation(relation: Relation): SingleResult = {
    val needGroupTo = relation.kind == Relation.Kind.Classic
    val isEmptyGroupTo = relation.groupTo.isEmpty
    val validatedRelation = if (!needGroupTo && !isEmptyGroupTo) relation.copy(groupTo = None) else relation

    for {
      _ <- ensure(!needGroupTo || !isEmptyGroupTo) {
        BadRequestError.Relation.GroupToMissed(validatedRelation.toString)
      }

      activeEvents <- eventDao.getList(
        optId = None,
        optStatus = Some(Event.Status.InProgress),
        optProjectId = Some(validatedRelation.project.id)
      ).lift

      _ <- ensure(activeEvents.total == 0) {
        ConflictError.Project.ActiveEventExists
      }
    } yield validatedRelation
  }
}
