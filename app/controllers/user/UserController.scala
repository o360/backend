package controllers.user

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
