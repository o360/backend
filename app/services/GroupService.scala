package services

import javax.inject.{Inject, Singleton}

import models.dao.GroupDao
import models.group.{Group => GroupModel}
import models.user.User
import org.davidbild.tristate.Tristate
import play.api.libs.concurrent.Execution.Implicits._
import utils.errors.{ApplicationError, ConflictError, NotFoundError}
import utils.listmeta.ListMeta

import scala.async.Async._
import scala.concurrent.Future

/**
  * Group service.
  */
@Singleton
class GroupService @Inject()(
  protected val groupDao: GroupDao
) extends ServiceResults[GroupModel] {

  /**
    * Returns group by ID
    */
  def getById(id: Long)(implicit account: User): SingleResult = async {
    await(groupDao.findById(id)) match {
      case Some(g) => g
      case None => NotFoundError.Group(id)
    }
  }

  /**
    * Returns groups list filtered by given criteria.
    *
    * @param parentId parent ID
    */
  def list(
    parentId: Tristate[Long]
  )(implicit account: User, meta: ListMeta): ListResult = async {
    val groups = await(groupDao.getList(
      id = None,
      parentId = parentId
    ))
    groups
  }

  /**
    * Creates new group.
    *
    * @param group group model
    */
  def create(group: GroupModel)(implicit account: User): SingleResult = async {
    await(checkParentId(group)) match {
      case Some(error) => error
      case None =>
        val created = await(groupDao.create(group))
        created
    }
  }

  /**
    * Updates group.
    *
    * @param draft group draft
    */
  def update(draft: GroupModel)(implicit account: User): SingleResult = async {
    await(getById(draft.id)) match {
      case Left(error) => error
      case Right(_) =>
        await(checkParentId(draft)) match {
          case Some(error) => error
          case None =>
            await(groupDao.update(draft))
            draft
        }
    }
  }

  /**
    * Removes group.
    *
    * @param id group ID
    */
  def delete(id: Long)(implicit account: User): UnitResult = async {
    await(getById(id)) match {
      case Left(error) => error
      case Right(_) =>
        val children = await(groupDao.findChildrenIds(id))
        if (children.nonEmpty) {
          ConflictError.Group.ChildrenExists(id, children)
        } else {
          val _ = await(groupDao.delete(id))
          unitResult
        }
    }
  }

  /**
    * Check if can set parent ID for group.
    * Possible errors: missed parent, self-reference, circular reference.
    *
    * @param group group model
    * @return either some error or none
    */
  private def checkParentId(group: GroupModel)(implicit account: User): Future[Option[ApplicationError]] = async {
    val groupIsNew = group.id == 0
    group.parentId match {
      case None => None
      case Some(parentId) if !groupIsNew && parentId == group.id =>
        Some(ConflictError.Group.ParentId(parentId))
      case Some(parentId) =>
        await(getById(parentId)) match {
          case Left(error) => Some(error)
          case _ =>
            if (groupIsNew) {
              None
            } else {
              val childrenIds = await(groupDao.findChildrenIds(group.id))
              if (childrenIds.contains(parentId)) {
                Some(ConflictError.Group.CircularReference(group.id, parentId))
              } else {
                None
              }
            }
        }
    }
  }
}
