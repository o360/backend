package models.dao

import javax.inject.Inject

import models.ListWithTotal
import models.user.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import utils.listmeta.ListMeta

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
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

  class UserTable(tag: Tag) extends Table[User](tag, "account") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[Option[String]]("name")
    def email = column[Option[String]]("email")
    def role = column[User.Role]("role")
    def status = column[User.Status]("status")

    def * = (id, name, email, role, status) <> ((User.apply _).tupled, User.unapply)
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
  with DaoHelper {

  import driver.api._

  /**
    * Returns user by ID.
    */
  def findById(id: Long): Future[Option[User]] = async {
    await(getList(id = Some(id))).data.headOption
  }


  /**
    * Returns list of users, filtered by given criteria.
    *
    * @param id user ID
    */
  def getList(
    id: Option[Long] = None,
    role: Option[User.Role] = None,
    status: Option[User.Status] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[User]] = {

    val query = Users
      .applyFilter { x =>
        Seq(
          id.map(x.id === _),
          role.map(x.role === _),
          status.map(x.status === _)
        )
      }

    runListQuery(query) {
      user => {
        case 'id => user.id
        case 'name => user.name
        case 'email => user.email
        case 'role => user.role
        case 'status => user.status
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
  def create(user: User, providerId: String, providerKey: String): Future[User] = async {
    val userId = await(db.run(Users.returning(Users.map(_.id)) += user))
    await(db.run(UserLogins += DbUserLogin(userId, providerId, providerKey)))
    user.copy(id = userId)
  }

  /**
    * Updates user.
    *
    * @param user user model
    * @return number of rows affected.
    */
  def update(user: User): Future[User] = async {
    await(db.run(Users.filter(_.id === user.id).update(user)))
    user
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
