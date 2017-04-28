package services

import javax.inject.{Inject, Singleton}

import models.dao.{EventDao, ProjectDao}
import models.event.Event
import models.project.Project
import models.user.User
import utils.errors.{ApplicationError, ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scalaz.Scalaz._
import scalaz._

/**
  * Project service.
  */
@Singleton
class ProjectService @Inject()(
  protected val projectDao: ProjectDao,
  protected val eventDao: EventDao
) extends ServiceResults[Project] {

  /**
    * Returns project by ID
    */
  def getById(id: Long)(implicit account: User): SingleResult = {
    projectDao.findById(id)
      .liftRight {
        NotFoundError.Project(id)
      }
  }

  /**
    * Returns projects list.
    */
  def getList(eventId: Option[Long])(implicit account: User, meta: ListMeta): ListResult =
    projectDao.getList(optId = None, optEventId = eventId).lift

  /**
    * Creates new project.
    *
    * @param project project model
    */
  def create(project: Project)(implicit account: User): SingleResult = {
    for {
      relations <- validateRelations(project.relations).lift
      created <- projectDao.create(project.copy(relations = relations)).lift(ExceptionHandler.sql)
    } yield created
  }

  /**
    * Updates project.
    *
    * @param draft project draft
    */
  def update(draft: Project)(implicit account: User): SingleResult = {
    for {
      _ <- getById(draft.id)

      relations <- validateRelations(draft.relations).lift

      activeEvents <- eventDao.getList(optStatus = Some(Event.Status.InProgress), optProjectId = Some(draft.id)).lift
      _ <- ensure(activeEvents.total == 0) {
        ConflictError.Project.ActiveEventExists
      }

      updated <- projectDao.update(draft.copy(relations = relations)).lift(ExceptionHandler.sql)
    } yield updated
  }

  /**
    * Removes project.
    *
    * @param id project ID
    */
  def delete(id: Long)(implicit account: User): UnitResult = {
    for {
      _ <- getById(id)
     _ <- projectDao.delete(id).lift(ExceptionHandler.sql)
    } yield ()
  }

  /**
    * Validate relations and returns either new relations or error.
    */
  private def validateRelations(relations: Seq[Project.Relation]): ApplicationError \/ Seq[Project.Relation] = {

    def validateRelation(relation: Project.Relation): ApplicationError \/ Project.Relation = {
      val needGroupTo = relation.kind == Project.RelationKind.Classic
      val isEmptyGroupTo = relation.groupTo.isEmpty

      if (needGroupTo && isEmptyGroupTo)
        ConflictError.Project.RelationGroupToMissed(relation.toString).left
      else if(!needGroupTo && !isEmptyGroupTo)
        relation.copy(groupTo = None).right
      else
        relation.right
    }

    relations
      .foldLeft(Seq.empty[Project.Relation].right[ApplicationError]) { (result, sourceRelation) =>
        for {
          relations <- result
          validatedRelation <- validateRelation(sourceRelation)
        } yield relations :+ validatedRelation
      }
  }
}
