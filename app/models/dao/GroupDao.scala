package models.dao

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.group.Group
import org.davidbild.tristate.Tristate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import utils.Transliteration
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

trait GroupComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  /**
    * Group DB model.
    */
  case class DbGroup(
    id: Long,
    parentId: Option[Long],
    name: String
  ) {
    def toModel(hasChildren: Boolean, level: Int) = Group(
      id,
      parentId,
      name,
      hasChildren,
      level
    )
  }

  object DbGroup {
    def fromModel(group: Group) = DbGroup(
      group.id,
      group.parentId,
      group.name
    )
  }

  class GroupTable(tag: Tag) extends Table[DbGroup](tag, "orgstructure") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def parentId = column[Option[Long]]("parent_id")
    def name = column[String]("name")

    def * = (id, parentId, name) <> ((DbGroup.apply _).tupled, DbGroup.unapply)
  }

  class GroupLevelView(tag: Tag) extends Table[(Long, Int)](tag, "orgstructure_level_view") {

    def group_id = column[Long]("group_id")
    def level = column[Int]("lvl")

    def * = (group_id, level)
  }

  val Groups = TableQuery[GroupTable]

  val GroupLevels = TableQuery[GroupLevelView]
}

/**
  * Group DAO.
  */
@Singleton
class GroupDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with GroupComponent
  with UserGroupComponent
  with DaoHelper {

  import profile.api._

  /**
    * Creates group.
    *
    * @param group group model
    * @return group model with ID
    */
  def create(group: Group): Future[Group] = {
    db.run(Groups.returning(Groups.map(_.id)) += DbGroup.fromModel(group))
      .flatMap(findById)
      .map(_.getOrElse(throw new NoSuchElementException("group not found")))
  }

  /**
    * Returns list of groups filtered by given criteria.
    *
    * @param optId       group ID
    * @param optParentId group parent ID
    * @param optUserId   only groups of the user
    * @param optName     part of group name
    */
  def getList(
    optId: Option[Long] = None,
    optParentId: Tristate[Long] = Tristate.Unspecified,
    optUserId: Option[Long] = None,
    optName: Option[String] = None,
    optLevels: Option[Seq[Int]] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Group]] = {

    def filterName(group: GroupTable) = optName.map { name =>
      val transliteratedName = Transliteration.transliterate(name)
      val nameLike = like(group.name, _: String, true)
      nameLike(name) || nameLike(transliteratedName)
    }

    def filterLevels(levelView: GroupLevelView) = optLevels.map { levels =>
      levels.foldLeft(false: Rep[Boolean])(_ || levelView.level === _)
    }

    val query = Groups
      .join(GroupLevels)
      .on(_.id === _.group_id)
      .applyFilter {
        case (group, levelView) =>
          Seq(
            optId.map(group.id === _),
            optParentId match {
              case Tristate.Present(parentId) => Some(group.parentId.fold(false: Rep[Boolean])(_ === parentId))
              case Tristate.Absent => Some(group.parentId.isEmpty)
              case Tristate.Unspecified => None
            },
            optUserId.map(userId => group.id in UserGroups.filter(_.userId === userId).map(_.groupId)),
            filterName(group),
            filterLevels(levelView)
          )
      }
      .map {
        case (group, levelView) =>
          val hasChildren = Groups.filter(_.parentId === group.id).exists
          (group, hasChildren, levelView.level)
      }

    runListQuery(query) {
      case (group, _, level) => {
        case 'id => group.id
        case 'name => group.name
        case 'level => level
      }
    }.map {
      case ListWithTotal(total, data) =>
        ListWithTotal(total, data.map { case (group, hasChildren, level) => group.toModel(hasChildren, level) })
    }
  }

  /**
    * Returns group by ID.
    *
    * @param id group ID
    * @return either some group or none
    */
  def findById(id: Long): Future[Option[Group]] = {
    getList(optId = Some(id)).map(_.data.headOption)
  }

  /**
    * Updates group.
    *
    * @param group group model
    * @return updated group
    */
  def update(group: Group): Future[Group] = {
    db.run(Groups.filter(_.id === group.id).update(DbGroup.fromModel(group))).map(_ => group)
  }

  /**
    * Removes group.
    *
    * @param id group ID
    * @return number of rows affected
    */
  def delete(id: Long): Future[Int] = db.run {
    Groups.filter(_.id === id).delete
  }

  /**
    * Returns group children IDs.
    *
    * @param id group ID
    */
  def findChildrenIds(id: Long): Future[Seq[Long]] = {
    val sql =
      sql"""
         WITH RECURSIVE T(id) AS (
           SELECT id FROM orgstructure g WHERE id = $id
           UNION ALL
           SELECT gc.id FROM T JOIN orgstructure gc ON gc.parent_id = T.id
         )
         SELECT  id
         FROM    T
         WHERE T.id <> $id
         """.as[Long]

    db.run(sql)
  }

  /**
    * Returns group ids for user (including all parent groups).
    *
    * @param userId user ID
    */
  def findGroupIdsByUserId(userId: Long): Future[Seq[Long]] = {
    val sql =
      sql"""
           WITH RECURSIVE T(id, parent_id) AS (
               SELECT id, parent_id
               FROM orgstructure g
               WHERE id IN(SELECT group_id FROM user_group WHERE user_id = $userId)
               UNION ALL
               SELECT gc.id, gc.parent_id
               FROM T JOIN orgstructure gc
               ON gc.id = T.parent_id
           )
           SELECT DISTINCT id
           FROM T
         """.as[Long]

    db.run(sql)
  }
}
