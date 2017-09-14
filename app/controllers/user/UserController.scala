package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.user.{ApiShortUser, ApiUser}
import controllers.authorization.{AllowedRole, AllowedStatus}
import models.user.{User, UserShort}
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

  implicit val sortingFields = Sorting.AvailableFields('id, 'name, 'gender)

  /**
    * Returns user by ID.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      userService
        .userGetById(id)
        .map(x => ApiShortUser(UserShort.fromUser(x)))
    }
  }

  /**
    * Returns list of users filtered by given filters.
    */
  def getList(name: Option[String]) = (silhouette.SecuredAction(AllowedStatus.approved) andThen ListAction).async {
    implicit request =>
      toResult(Ok) {
        userService
          .list(None, Some(User.Status.Approved), Tristate.Unspecified, name)
          .map { users =>
            Response.List(users) { user =>
              ApiShortUser(UserShort.fromUser(user))
            }
          }
      }
  }

  /**
    * Returns logged in user.
    */
  def me = silhouette.SecuredAction { request =>
    toResult(ApiUser(request.identity))
  }

  /**
    * Updates user.
    */
  def update = silhouette.SecuredAction.async(parse.json[ApiUser]) { implicit request =>
    toResult(Ok) {
      val draft = request.body.copy(id = request.identity.id)
      userService
        .update(draft.toModel)
        .map(ApiUser(_))
    }
  }
}
