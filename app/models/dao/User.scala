package models.dao

import javax.inject.Inject

import models.ListWithTotal
import models.user.{User => UserModel}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.Future
import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

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

  implicit lazy val roleColumnType = MappedColumnType.base[UserModel.Role, Byte](
    {
      case UserModel.Role.User => 0
      case UserModel.Role.Admin => 1
    }, {
      case 0 => UserModel.Role.User
      case 1 => UserModel.Role.Admin
    }
  )

  implicit lazy val statusColumnType = MappedColumnType.base[UserModel.Status, Byte](
    {
      case UserModel.Status.New => 0
      case UserModel.Status.Approved => 1
    }, {
      case 0 => UserModel.Status.New
      case 1 => UserModel.Status.Approved
    }
  )

  class UserTable(tag: Tag) extends Table[UserModel](tag, "account") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[Option[String]]("name")
    def email = column[Option[String]]("email")
    def role = column[UserModel.Role]("role")
    def status = column[UserModel.Status]("status")

    def * = (id, name, email, role, status) <> ((UserModel.apply _).tupled, UserModel.unapply)
  }

  val Users = TableQuery[UserTable]
}

/**
  * DAO for User model.
  */
class User @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with UserComponent
  with UserLoginComponent
  with DaoHelper {

  import driver.api._

  /**
    * Returns user by ID.
    */
  def findById(id: Long): Future[Option[UserModel]] = async {
    await(get(id = Some(id))).data.headOption
  }


  /**
    * Returns list of users, filtered by given criteria.
    *
    * @param id user ID
    */
  def get(
    id: Option[Long] = None,
    role: Option[UserModel.Role] = None,
    status: Option[UserModel.Status] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[UserModel]] = {

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
  def findByProvider(providerId: String, providerKey: String): Future[Option[UserModel]] = db.run {
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
  def create(user: UserModel, providerId: String, providerKey: String): Future[Long] = async {
    val userId = await(db.run(Users.returning(Users.map(_.id)) += user))
    await(db.run(UserLogins += DbUserLogin(userId, providerId, providerKey)))
    userId
  }

  /**
    * Updates user.
    *
    * @param user user model
    * @return number of rows affected.
    */
  def update(user: UserModel): Future[Int] = db.run {
    Users.filter(_.id === user.id).update(user)
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
