package models.dao

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models.invite.Invite
import models.{ListWithTotal, NamedEntity}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

/**
  * Component for invite and invite_group tables.
  */
trait InviteComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  case class DbInvite(
    id: Long,
    code: String,
    email: String,
    activationTime: Option[Timestamp],
    creationTime: Timestamp
  ) {
    def toModel(groups: Seq[NamedEntity]) = Invite(
      code,
      email,
      groups.toSet,
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
  val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with DaoHelper
  with InviteComponent
  with GroupComponent {

  import profile.api._

  def getList(code: Option[String] = None, activated: Option[Boolean] = None)(
    implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Invite]] = {

    val baseQuery = Invites.applyFilter(
      invite =>
        Seq(
          code.map(invite.code === _),
          activated.map(if (_) invite.activationTime.nonEmpty else invite.activationTime.isEmpty)
      )
    )

    val sortMapping: (InviteTable) => PartialFunction[Symbol, Rep[_]] = invite => {
      case 'code => invite.code
      case 'email => invite.email
      case 'activationTime => invite.activationTime
      case 'creationTime => invite.creationTime
    }
    val inviteQuery = baseQuery
      .applySorting(meta.sorting)(sortMapping)
      .applyPagination(meta.pagination)
      .joinLeft {
        InviteGroups.join(Groups).on(_.groupId === _.id)
      }
      .on(_.id === _._1.inviteId)
      .applySorting(meta.sorting)(x => sortMapping(x._1))

    for {
      count <- db.run(baseQuery.length.result)
      flatResult <- if (count > 0) db.run(inviteQuery.result) else Nil.toFuture
    } yield {
      val data = flatResult
        .groupByWithOrder { case (invite, _) => invite }
        .map {
          case (invite, groupIdsWithEvent) =>
            val groups = groupIdsWithEvent
              .collect { case (_, Some((_, g))) => NamedEntity(g.id, g.name) }
            invite.toModel(groups)
        }

      ListWithTotal(count, data)
    }
  }

  def create(invite: Invite): Future[Invite] = {
    val actions = for {
      inviteId <- Invites.returning(Invites.map(_.id)) += DbInvite.fromModel(invite)
      _ <- InviteGroups ++= invite.groups.map(x => DbInviteGroup(inviteId, x.id))
    } yield invite

    db.run(actions.transactionally)
  }

  def findByCode(code: String): Future[Option[Invite]] = getList(Some(code)).map(_.data.headOption)

  def activate(code: String, time: Timestamp): Future[Unit] = {
    db.run(Invites.filter(_.code === code).map(_.activationTime).update(Some(time))).map(_ => ())
  }
}
