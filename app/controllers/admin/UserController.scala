package controllers.admin

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.user.ApiUser
import controllers.authorization.AllowedRole
import org.davidbild.tristate.Tristate
import play.api.mvc.ControllerComponents
import services.UserService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * User controller.
  */
@Singleton
class UserController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val userService: UserService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'name, 'email, 'role, 'status, 'gender)

  /**
    * Returns user by ID.
    */
  def getById(id: Long) = silhouette.SecuredAction.async { implicit request =>
    toResult(Ok) {
      userService
        .getById(id)
        .map(ApiUser(_))
    }
  }

  /**
    * Returns list of users filtered by given filters.
    */
  def getList(
    role: Option[ApiUser.ApiRole],
    status: Option[ApiUser.ApiStatus],
    groupId: Tristate[Long],
    name: Option[String]
  ) = (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
    toResult(Ok) {
      userService
        .list(role.map(_.value), status.map(_.value), groupId, name)
        .map { users =>
          Response.List(users) { user =>
            ApiUser(user)
          }
        }
    }
  }

  /**
    * Updates user.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiUser]) { implicit request =>
    toResult(Ok) {
      val draft = request.body.copy(id = id)
      userService
        .update(draft.toModel)
        .map(ApiUser(_))
    }
  }

  /**
    * Deletes user.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    userService
      .delete(id)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }
}
