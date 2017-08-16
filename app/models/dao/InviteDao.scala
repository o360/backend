package models.dao

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.invite.Invite
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Component for invite and invite_group tables.
  */
trait InviteComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  case class DbInvite(
    id: Long,
    code: String,
    email: String,
    activationTime: Option[Timestamp],
    creationTime: Timestamp
  ) {
    def toModel(groupIds: Seq[Long]) = Invite(
      code,
      email,
      groupIds.toSet,
      activationTime,
      creationTime
    )
  }

  object DbInvite {
    def fromModel(invite: Invite): DbInvite = DbInvite(
      0,
      invite.code,
      invite.email,
      invite.activationTime,
      invite.creationTime
    )
  }

  class InviteTable(tag: Tag) extends Table[DbInvite](tag, "invite") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def code = column[String]("code")
    def email = column[String]("email")
    def activationTime = column[Option[Timestamp]]("activation_time")
    def creationTime = column[Timestamp]("creation_time")

    def * = (id, code, email, activationTime, creationTime) <> ((DbInvite.apply _).tupled, DbInvite.unapply)
  }

  val Invites = TableQuery[InviteTable]

  case class DbInviteGroup(
    inviteId: Long,
    groupId: Long
  )

  class InviteGroupTable(tag: Tag) extends Table[DbInviteGroup](tag, "invite_group") {

    def inviteId = column[Long]("invite_id", O.PrimaryKey)
    def groupId = column[Long]("group_id", O.PrimaryKey)

    def * = (inviteId, groupId) <> ((DbInviteGroup.apply _).tupled, DbInviteGroup.unapply)
  }

  val InviteGroups = TableQuery[InviteGroupTable]
}

/**
  * Invite DAO.
  */
@Singleton
class InviteDao @Inject()(
  val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with DaoHelper
  with InviteComponent {

  import driver.api._

  def getList(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Invite]] = {

    val baseQuery = Invites

    val inviteQuery = baseQuery
      .sortBy(_.id)
      .applyPagination(meta.pagination)
      .joinLeft(InviteGroups)
      .on(_.id === _.inviteId)
      .sortBy(_._1.id)

    for {
      count <- db.run(baseQuery.length.result)
      flatResult <- if (count > 0) db.run(inviteQuery.result) else Nil.toFuture
    } yield {
      val data = flatResult
        .groupByWithOrder { case (invite, _) => invite }
        .map {
          case (invite, groupIdsWithEvent) =>
            val groupIds = groupIdsWithEvent
              .collect { case (_, Some(g)) => g.groupId }
            invite.toModel(groupIds)
        }

      ListWithTotal(count, data)
    }
  }

  def create(invite: Invite): Future[Invite] = {
    val actions = for {
      inviteId <- Invites.returning(Invites.map(_.id)) += DbInvite.fromModel(invite)
      _ <- InviteGroups ++= invite.groupIds.map(DbInviteGroup(inviteId, _))
    } yield invite

    db.run(actions.transactionally)
  }

  def findByCode(code: String): Future[Option[Invite]] = {
    val inviteQuery = Invites
      .filter(_.code === code)
      .joinLeft(InviteGroups)
      .on(_.id === _.inviteId)
      .result

    db.run(inviteQuery).map { result =>
      result
        .groupByWithOrder(_._1)
        .map {
          case (dbInvite, groups) =>
            dbInvite.toModel(groups.flatMap(_._2.map(_.groupId)))
        }
        .headOption
    }
  }

  def activate(code: String, time: Timestamp): Future[Unit] = {
    db.run(Invites.filter(_.code === code).map(_.activationTime).update(Some(time))).map(_ => ())
  }
}
