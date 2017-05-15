package services

import javax.inject.{Inject, Singleton}

import models.dao.{GroupDao, UserGroupDao}
import models.group.{Group => GroupModel}
import models.user.User
import org.davidbild.tristate.Tristate
import play.api.libs.concurrent.Execution.Implicits._
import utils.errors.{ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

/**
  * Group service.
  */
@Singleton
class GroupService @Inject()(
  protected val groupDao: GroupDao,
  protected val userGroupDao: UserGroupDao
) extends ServiceResults[GroupModel] {

  /**
    * Returns group by ID
    */
  def getById(id: Long)(implicit account: User): SingleResult = {
    groupDao.findById(id)
      .liftRight {
        NotFoundError.Group(id)
      }
  }

  /**
    * Returns groups list filtered by given criteria.
    *
    * @param parentId parent ID
    * @param userId   only groups of user
    */
  def list(
    parentId: Tristate[Long],
    userId: Option[Long]
  )(implicit account: User, meta: ListMeta): ListResult = {
    groupDao.getList(
      optId = None,
      optParentId = parentId,
      optUserId = userId
    ).lift
  }

  /**
    * Creates new group.
    *
    * @param group group model
    */
  def create(group: GroupModel)(implicit account: User): SingleResult = {
    for {
      _ <- validateParentId(group)
      created <- groupDao.create(group).lift
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

      updated <- groupDao.update(draft).lift
    } yield updated
  }

  /**
    * Removes group.
    *
    * @param id group ID
    */
  def delete(id: Long)(implicit account: User): UnitResult = {
    for {
      _ <- getById(id)

      _ <- groupDao.delete(id).lift(ExceptionHandler.sql)
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
