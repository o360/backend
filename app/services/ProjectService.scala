package services

import javax.inject.{Inject, Singleton}

import models.dao.ProjectDao
import models.project.Project
import models.user.User
import utils.errors.{ApplicationError, ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scalaz._
import Scalaz._

/**
  * Project service.
  */
@Singleton
class ProjectService @Inject()(
  protected val projectDao: ProjectDao
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
  def getList()(implicit account: User, meta: ListMeta): ListResult = projectDao.getList().lift

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
     _ <- projectDao.delete(id).lift
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
