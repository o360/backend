package models.dao

import javax.inject.Inject

import models.ListWithTotal
import models.user.{Role => RoleModel, Status => StatusModel, User => UserModel}
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

  implicit lazy val roleColumnType = MappedColumnType.base[RoleModel, Byte](
    {
      case RoleModel.User => 0
      case RoleModel.Admin => 1
    }, {
      case 0 => RoleModel.User
      case 1 => RoleModel.Admin
    }
  )

  implicit lazy val statusColumnType = MappedColumnType.base[StatusModel, Byte](
    {
      case StatusModel.New => 0
      case StatusModel.Approved => 1
    }, {
      case 0 => StatusModel.New
      case 1 => StatusModel.Approved
    }
  )

  class UserTable(tag: Tag) extends Table[UserModel](tag, "account") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[Option[String]]("name")
    def email = column[Option[String]]("email")
    def role = column[RoleModel]("role")
    def status = column[StatusModel]("status")

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
    id: Option[Long]
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[UserModel]] = {
    Users
      .applyFilter { x =>
        Seq(
          id.map(x.id === _)
        )
      }
      .toListResult {
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

}
