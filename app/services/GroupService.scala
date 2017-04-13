package services

import javax.inject.{Inject, Singleton}

import models.dao.{GroupDao, UserGroupDao}
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
  protected val groupDao: GroupDao,
  protected val userGroupDao: UserGroupDao
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
    * @param userId   only groups of user
    */
  def list(
    parentId: Tristate[Long],
    userId: Option[Long]
  )(implicit account: User, meta: ListMeta): ListResult = async {
    val groups = await(groupDao.getList(
      id = None,
      parentId = parentId,
      userId = userId
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

    def doUpdate(): Future[Option[ApplicationError]] = async {
      await(checkParentId(draft)) match {
        case Some(error) => Some(error)
        case None =>
          await(groupDao.update(draft))
          None
      }
    }

    await(getById(draft.id)) match {
      case Left(error) => error
      case _ =>
        val maybeError = await(doUpdate())
        if (maybeError.nonEmpty) {
          maybeError.get
        } else {
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
        } else if (await(userGroupDao.exists(groupId = Some(id)))) {
          ConflictError.Group.UserExists(id)
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

    def isSelfReference(parentId: Long) = !groupIsNew && parentId == group.id

    def isCircularReference(parentId: Long) = async {
      val childrenIds = await(groupDao.findChildrenIds(group.id))
      childrenIds.contains(parentId)
    }

    def doParentIdCheck(parentId: Long) = async {
      await(getById(parentId)) match {
        case Left(error) => Some(error)
        case _ if groupIsNew => None
        case _ =>
          if (await(isCircularReference(parentId))) {
            Some(ConflictError.Group.CircularReference(group.id, parentId))
          } else {
            None
          }
      }
    }

    group.parentId match {
      case None => None
      case Some(parentId) =>
        if (isSelfReference(parentId)) {
          Some(ConflictError.Group.ParentId(group.parentId.get))
        } else {
          await(doParentIdCheck(parentId))
        }
    }
  }
}
