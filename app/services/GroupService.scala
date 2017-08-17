package services

import javax.inject.{Inject, Singleton}

import models.dao.{GroupDao, ProjectDao, ProjectRelationDao, UserGroupDao}
import models.group.{Group => GroupModel}
import models.project.{Project, Relation}
import models.user.User
import org.davidbild.tristate.Tristate
import utils.errors.{ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Group service.
  */
@Singleton
class GroupService @Inject()(
  protected val groupDao: GroupDao,
  protected val userGroupDao: UserGroupDao,
  protected val relationDao: ProjectRelationDao,
  protected val projectDao: ProjectDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[GroupModel] {

  /**
    * Returns group by ID
    */
  def getById(id: Long)(implicit account: User): SingleResult = {
    groupDao
      .findById(id)
      .liftRight {
        NotFoundError.Group(id)
      }
  }

  /**
    * Returns groups list filtered by given criteria.
    *
    * @param parentId parent ID
    * @param userId   only groups of user
    * @param name     part of group name
    */
  def list(
    parentId: Tristate[Long],
    userId: Option[Long],
    name: Option[String],
    levels: Option[String]
  )(implicit account: User, meta: ListMeta): ListResult = {

    val levelsParsed = levels.flatMap { l =>
      Try(l.split(",")).map(_.toSeq.map(_.toInt)).toOption
    }

    groupDao
      .getList(
        optId = None,
        optParentId = parentId,
        optUserId = userId,
        optName = name,
        optLevels = levelsParsed
      )
      .lift
  }

  /**
    * Creates new group.
    *
    * @param group group model
    */
  def create(group: GroupModel)(implicit account: User): SingleResult = {
    for {
      _ <- validateParentId(group)
      created <- groupDao.create(group).lift(ExceptionHandler.sql)
    } yield created
  }

  /**
    * Updates group.
    *
    * @param draft group draft
    */
  def update(draft: GroupModel)(implicit account: User): SingleResult = {
    for {
      _ <- getById(draft.id)
      _ <- validateParentId(draft)

      updated <- groupDao.update(draft).lift(ExceptionHandler.sql)
    } yield updated
  }

  /**
    * Removes group.
    *
    * @param id group ID
    */
  def delete(id: Long)(implicit account: User): UnitResult = {

    def getConflictedEntities = {
      for {
        projects <- projectDao.getList(optGroupAuditorId = Some(id))
        relationsWithGroupFrom <- relationDao.getList(optGroupFromId = Some(id))
        relationsWithGroupTo <- relationDao.getList(optGroupToId = Some(id))
      } yield {
        val allProjects =
          (projects.data.map(_.toNamedEntity) ++
            relationsWithGroupFrom.data.map(_.project) ++
            relationsWithGroupTo.data.map(_.project)).distinct
        ConflictError.getConflictedEntitiesMap(
          Project.namePlural -> allProjects
        )
      }
    }

    for {
      _ <- getById(id)

      conflictedEntities <- getConflictedEntities.lift
      _ <- ensure(conflictedEntities.isEmpty) {
        ConflictError.General(Some(GroupModel.nameSingular), conflictedEntities)
      }

      _ <- groupDao.delete(id).lift
    } yield ()
  }

  /**
    * Check if can set parent ID for group.
    * Possible errors: missed parent, self-reference, circular reference.
    *
    * @param group group model
    * @return either error or unit
    */
  private def validateParentId(group: GroupModel)(implicit account: User): UnitResult = {
    val groupIsNew = group.id == 0

    group.parentId match {
      case None => ().lift
      case Some(parentId) =>
        for {
          _ <- ensure(groupIsNew || parentId != group.id) {
            ConflictError.Group.ParentId(group.id)
          }

          _ <- getById(parentId)

          isCircularReference <- {
            !groupIsNew.toFuture && groupDao.findChildrenIds(group.id).map(_.contains(parentId))
          }.lift

          _ <- ensure(!isCircularReference) {
            ConflictError.Group.CircularReference(group.id, parentId)
          }
        } yield ()
    }
  }
}
