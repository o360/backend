package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.Response
import controllers.api.user.ApiUser
import controllers.authorization.AllowedRole
import org.davidbild.tristate.Tristate
import play.api.libs.concurrent.Execution.Implicits._
import services.UserService
import silhouette.DefaultEnv
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.async.Async._

/**
  * User controller.
  */
@Singleton
class UserController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val userService: UserService
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'name, 'email, 'role, 'status)

  /**
    * Returns user by ID.
    */
  def getById(id: Long) = silhouette.SecuredAction.async { implicit request =>
    async {
      toResult(Ok) {
        for {
          user <- await(userService.getById(id)).right
        } yield ApiUser(user)
      }
    }
  }

  /**
    * Returns list of users filtered by given filters.
    */
  def getList(
    role: Option[ApiUser.ApiRole],
    status: Option[ApiUser.ApiStatus],
    groupId: Tristate[Long]
  ) = (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
    async {
      toResult(Ok) {
        val users = await(userService.list(
          role.map(_.value),
          status.map(_.value),
          groupId
        ))
        Response.List(users) {
          ApiUser(_)
        }
      }
    }
  }

  /**
    * Updates user.
    */
  def update(id: Long) = silhouette.SecuredAction.async(parse.json[ApiUser]) { implicit request =>
    async {
      toResult(Ok) {
        val draft = request.body.copy(id = id)
        for {
          updatedUser <- await(userService.update(draft.toModel)).right
        } yield ApiUser(updatedUser)
      }
    }
  }

  /**
    * Deletes user.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    async {
      await(userService.delete(id)) match {
        case Left(error) => toResult(error)
        case Right(_) => NoContent
      }
    }
  }
}
