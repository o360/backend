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

  class UserTable(tag: Tag) extends Table[User](tag, "account") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[Option[String]]("name")
    def email = column[Option[String]]("email")
    def gender = column[Option[User.Gender]]("gender")
    def role = column[User.Role]("role")
    def status = column[User.Status]("status")

    def * = (id, name, email, gender, role, status) <> ((User.apply _).tupled, User.unapply)
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
    * @param optId      user ID
    * @param optRole    user role
    * @param optStatus  user status
    * @param optGroupId only users of the group
    */
  def getList(
    optId: Option[Long] = None,
    optRole: Option[User.Role] = None,
    optStatus: Option[User.Status] = None,
    optGroupId: Tristate[Long] = Tristate.Unspecified
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[User]] = {

    val query = Users
      .applyFilter { x =>
        Seq(
          optId.map(x.id === _),
          optRole.map(x.role === _),
          optStatus.map(x.status === _),
          optGroupId match {
            case Tristate.Unspecified => None
            case Tristate.Absent => Some(!(x.id in UserGroups.map(_.userId)))
            case Tristate.Present(groupId) => Some(x.id in UserGroups.filter(_.groupId === groupId).map(_.userId))
          }
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
    }
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
      userId <- db.run(Users.returning(Users.map(_.id)) += user)
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
    db.run(Users.filter(_.id === user.id).update(user)).map(_ => user)
  }

  /**
    * Deletes user.
    *
    * @param id user ID
    * @return number of rows affected
    */
  def delete(id: Long): Future[Int] = db.run {
    Users.filter(_.id === id).delete
  }
}
