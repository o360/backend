package models.dao

import javax.inject.Inject

import models.ListWithTotal
import models.user.User
import org.davidbild.tristate.Tristate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Component for 'user_login' table.
  */
trait UserLoginComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

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
trait UserMetaComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

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
trait UserComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  implicit lazy val roleColumnType = MappedColumnType.base[User.Role, Byte](
    {
      case User.Role.User => 0
      case User.Role.Admin => 1
    }, {
      case 0 => User.Role.User
      case 1 => User.Role.Admin
    }
  )

  implicit lazy val statusColumnType = MappedColumnType.base[User.Status, Byte](
    {
      case User.Status.New => 0
      case User.Status.Approved => 1
    }, {
      case 0 => User.Status.New
      case 1 => User.Status.Approved
    }
  )

  implicit lazy val genderColumnType = MappedColumnType.base[User.Gender, Byte](
    {
      case User.Gender.Male => 0
      case User.Gender.Female => 1
    }, {
      case 0 => User.Gender.Male
      case 1 => User.Gender.Female
    }
  )

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
    isDeleted: Boolean
  ) {
    def toModel = User(
      id,
      name,
      email,
      gender,
      role,
      status
    )
  }

  object DbUser {
    def fromModel(user: User) = DbUser(
      user.id,
      user.name,
      user.email,
      user.gender,
      user.role,
      user.status,
      isDeleted = false
    )
  }

  class UserTable(tag: Tag) extends Table[DbUser](tag, "account") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[Option[String]]("name")
    def email = column[Option[String]]("email")
    def gender = column[Option[User.Gender]]("gender")
    def role = column[User.Role]("role")
    def status = column[User.Status]("status")
    def isDeleted = column[Boolean]("is_deleted")

    def * = (id, name, email, gender, role, status, isDeleted) <> ((DbUser.apply _).tupled, DbUser.unapply)
  }

  val Users = TableQuery[UserTable]
}

/**
  * DAO for User model.
  */
class UserDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with UserComponent
  with UserLoginComponent
  with UserGroupComponent
  with UserMetaComponent
  with DaoHelper {

  import driver.api._

  /**
    * Returns user by ID.
    */
  def findById(id: Long): Future[Option[User]] = {
    getList(optId = Some(id)).map(_.data.headOption)
  }


  /**
    * Returns list of users, filtered by given criteria.
    *
    * @param optId       user ID
    * @param optRole     user role
    * @param optStatus   user status
    * @param optGroupIds only users of the groups
    * @param optName     name containing string
    */
  def getList(
    optId: Option[Long] = None,
    optRole: Option[User.Role] = None,
    optStatus: Option[User.Status] = None,
    optGroupIds: Tristate[Seq[Long]] = Tristate.Unspecified,
    optName: Option[String] = None,
    includeDeleted: Boolean = false
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[User]] = {

    def filterGroup(user: UserTable) = optGroupIds match {
      case Tristate.Unspecified => None
      case Tristate.Absent => Some(!(user.id in UserGroups.map(_.userId)))
      case Tristate.Present(groupIds) =>
        Some(user.id in UserGroups.filter(_.groupId inSet groupIds.distinct).map(_.userId))
    }

    def filterName(user: UserTable) = optName.map { name =>
      val escapedName = name.replace("%", "\\%") // postgres specific % escape
      user.name.fold(false: Rep[Boolean])(_.like(s"%$escapedName%"))
    }

    def deletedFilter(user: UserTable) = if (includeDeleted) None else Some(!user.isDeleted)

    val query = Users
      .applyFilter { x =>
        Seq(
          optId.map(x.id === _),
          optRole.map(x.role === _),
          optStatus.map(x.status === _),
          filterGroup(x),
          filterName(x),
          deletedFilter(x)
        )
      }

    runListQuery(query) {
      user => {
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
      .join(UserLogins).on(_.id === _.userId)
      .filter { case (user, userLogin) =>
        userLogin.providerId === providerId && userLogin.providerKey === providerKey
      }
      .map { case (user, userLogin) =>
        user
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
      _ <- Users.filter(_.id === id).map(_.isDeleted).update(true)
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
