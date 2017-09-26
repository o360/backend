package models.dao

import java.time.{ZoneId, ZoneOffset}
import javax.inject.Inject

import models.ListWithTotal
import models.user.User
import org.davidbild.tristate.Tristate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import utils.{Logger, Transliteration}
import utils.listmeta.ListMeta
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Component for 'user_login' table.
  */
trait UserLoginComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  case class DbUserLogin(userId: Long, providerId: String, providerKey: String)

  class UserLoginTable(tag: Tag) extends Table[DbUserLogin](tag, "user_login") {

    def userId = column[Long]("user_id")
    def providerId = column[String]("provider_id")
    def providerKey = column[String]("provider_key")

    def * = (userId, providerId, providerKey) <> ((DbUserLogin.apply _).tupled, DbUserLogin.unapply)
  }

  val UserLogins = TableQuery[UserLoginTable]
}

/**
  * Component for user_meta table.
  */
trait UserMetaComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  case class DbUserMeta(userId: Long, gdriveFolderId: Option[String])

  class UserMetaTable(tag: Tag) extends Table[DbUserMeta](tag, "user_meta") {
    def userId = column[Long]("user_id")
    def gdriveFolderId = column[Option[String]]("gdrive_folder_id")

    def * = (userId, gdriveFolderId) <> ((DbUserMeta.apply _).tupled, DbUserMeta.unapply)
  }

  val UserMetas = TableQuery[UserMetaTable]
}

/**
  * Component for 'account' table.
  */
trait UserComponent extends Logger with EnumColumnMapper { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  implicit lazy val roleColumnType = mappedEnumSeq[User.Role](User.Role.User, User.Role.Admin)

  implicit lazy val statusColumnType = mappedEnumSeq[User.Status](User.Status.New, User.Status.Approved)

  implicit lazy val genderColumnType = mappedEnumSeq[User.Gender](User.Gender.Male, User.Gender.Female)

  /**
    * Database user model.
    */
  case class DbUser(
    id: Long,
    name: Option[String],
    email: Option[String],
    gender: Option[User.Gender],
    role: User.Role,
    status: User.Status,
    timezone: String,
    termsApproved: Boolean,
    pictureName: Option[String],
    isDeleted: Boolean
  ) {
    def toModel =
      this
        .into[User]
        .withFieldConst(_.timezone, Try(ZoneId.of(timezone)).getOrElse {
          log.error(s"User $id has incorrect timezone $timezone, UTC used instead")
          ZoneOffset.UTC
        })
        .transform
  }

  object DbUser {
    def fromModel(user: User) =
      user
        .into[DbUser]
        .withFieldComputed(_.timezone, _.timezone.getId)
        .withFieldConst(_.isDeleted, false)
        .transform
  }

  class UserTable(tag: Tag) extends Table[DbUser](tag, "account") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[Option[String]]("name")
    def email = column[Option[String]]("email")
    def gender = column[Option[User.Gender]]("gender")
    def role = column[User.Role]("role")
    def status = column[User.Status]("status")
    def timezone = column[String]("timezone")
    def termsApproved = column[Boolean]("terms_approved")
    def pictureName = column[Option[String]]("picture_name")
    def isDeleted = column[Boolean]("is_deleted")

    def * =
      (id, name, email, gender, role, status, timezone, termsApproved, pictureName, isDeleted) <> ((DbUser.apply _).tupled, DbUser.unapply)
  }

  val Users = TableQuery[UserTable]
}

/**
  * DAO for User model.
  */
class UserDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with UserComponent
  with UserLoginComponent
  with UserGroupComponent
  with UserMetaComponent
  with ActiveProjectComponent
  with DaoHelper {

  import profile.api._

  /**
    * Returns user by ID.
    */
  def findById(id: Long): Future[Option[User]] = {
    getList(optIds = Some(Seq(id))).map(_.data.headOption)
  }

  /**
    * Returns list of users, filtered by given criteria.
    */
  def getList(
    optIds: Option[Seq[Long]] = None,
    optRole: Option[User.Role] = None,
    optStatus: Option[User.Status] = None,
    optGroupIds: Tristate[Seq[Long]] = Tristate.Unspecified,
    optName: Option[String] = None,
    optEmail: Option[String] = None,
    optProjectIdAuditor: Option[Long] = None,
    includeDeleted: Boolean = false
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[User]] = {

    def filterGroup(user: UserTable) = optGroupIds match {
      case Tristate.Unspecified => None
      case Tristate.Absent => Some(!(user.id in UserGroups.map(_.userId)))
      case Tristate.Present(groupIds) =>
        Some(user.id in UserGroups.filter(_.groupId inSet groupIds.distinct).map(_.userId))
    }

    def filterName(user: UserTable) = optName.map { name =>
      user.name.fold(false: Rep[Boolean]) { nameColumn =>
        val transliteratedName = Transliteration.transliterate(name)
        val nameLike = like(nameColumn, _: String, true)
        nameLike(name) || nameLike(transliteratedName)
      }
    }

    def filterActiveProject(user: UserTable) = optProjectIdAuditor.map { projectIdAuditor =>
      user.id.in(ActiveProjectsAuditors.filter(_.activeProjectId === projectIdAuditor).map(_.userId))
    }

    def deletedFilter(user: UserTable) = if (includeDeleted) None else Some(!user.isDeleted)

    val query = Users
      .applyFilter { x =>
        Seq(
          optIds.map(x.id.inSet(_)),
          optRole.map(x.role === _),
          optStatus.map(x.status === _),
          filterGroup(x),
          filterName(x),
          optEmail.map(email => x.email.fold(false: Rep[Boolean])(_ === email)),
          filterActiveProject(x),
          deletedFilter(x)
        )
      }

    runListQuery(query) { user =>
      {
        case 'id => user.id
        case 'name => user.name
        case 'email => user.email
        case 'role => user.role
        case 'status => user.status
        case 'gender => user.gender
      }
    }.map { case ListWithTotal(total, data) => ListWithTotal(total, data.map(_.toModel)) }
  }

  /**
    * Returns user associated with provider key.
    *
    * @param providerId  provider ID
    * @param providerKey provider key
    */
  def findByProvider(providerId: String, providerKey: String): Future[Option[User]] = db.run {
    Users
      .join(UserLogins)
      .on(_.id === _.userId)
      .filter {
        case (_, userLogin) => userLogin.providerId === providerId && userLogin.providerKey === providerKey
      }
      .map {
        case (user, _) => user
      }
      .result
      .headOption
      .map(_.map(_.toModel))
  }

  /**
    * Creates new user and associate it with provider key.
    *
    * @param user        user to create
    * @param providerId  provider ID
    * @param providerKey provider key
    */
  def create(user: User, providerId: String, providerKey: String): Future[User] = {
    for {
      userId <- db.run(Users.returning(Users.map(_.id)) += DbUser.fromModel(user))
      _ <- db.run(UserLogins += DbUserLogin(userId, providerId, providerKey))
    } yield user.copy(id = userId)
  }

  /**
    * Updates user.
    *
    * @param user user model
    * @return number of rows affected.
    */
  def update(user: User): Future[User] = {
    db.run(Users.filter(_.id === user.id).update(DbUser.fromModel(user))).map(_ => user)
  }

  /**
    * Deletes login info and marks user as deleted.
    *
    * @param id user ID
    */
  def delete(id: Long): Future[Unit] = {
    val actions = for {
      _ <- Users.filter(_.id === id).map(x => (x.isDeleted, x.pictureName)).update((true, None))
      _ <- UserLogins.filter(_.userId === id).delete
    } yield ()
    db.run(actions.transactionally)
  }

  /**
    * Sets users gdrive folder ID.
    *
    * @param userId   ID of user
    * @param folderId gdrive folder ID
    */
  def setGdriveFolderId(userId: Long, folderId: String): Future[String] = {
    val actions = for {
      exists <- UserMetas.filter(_.userId === userId).exists.result
      _ <- if (exists) UserMetas.filter(_.userId === userId).update(DbUserMeta(userId, Some(folderId)))
      else UserMetas += DbUserMeta(userId, Some(folderId))
    } yield ()

    db.run(actions.transactionally).map(_ => folderId)
  }

  /**
    * Gets users gdrive folder ID.
    *
    * @param userId ID of user
    * @return some folder ID
    */
  def getGdriveFolder(userId: Long): Future[Option[String]] = {
    val query = UserMetas
      .filter(x => x.userId === userId && x.gdriveFolderId.nonEmpty)
      .map(_.gdriveFolderId)
      .result
      .headOption

    db.run(query).map(_.flatten)
  }
}
