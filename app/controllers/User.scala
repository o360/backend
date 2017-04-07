package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.user.{RoleFormat, StatusFormat, UserFormat}
import controllers.api.{ListResponse, NoContentResponse}
import controllers.authorization.WithRole
import services.{User => UserService}
import silhouette.DefaultEnv
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * User controller.
  */
@Singleton
class User @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val userService: UserService
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'name, 'email, 'role, 'status)

  /**
    * Returns user by ID.
    */
  def get(id: Long) = silhouette.SecuredAction.async { implicit request =>
    async {
      toResult {
        for {
          user <- await(userService.getById(id)).right
        } yield UserFormat(user)
      }
    }
  }

  /**
    * Returns list of users filtered by given filters.
    */
  def list(
    role: Option[RoleFormat],
    status: Option[StatusFormat]
  ) = (silhouette.SecuredAction(WithRole.admin) andThen ListAction).async { implicit request =>
    async {
      toResult {
        val users = await(userService.list(
          role.map(_.value),
          status.map(_.value))
        )
        ListResponse(users) {
          UserFormat(_)
        }
      }
    }
  }

  /**
    * Updates user.
    */
  def update(id: Long) = silhouette.SecuredAction.async(parse.json[UserFormat]) { implicit request =>
    async {
      toResult {
        val draft = request.body.copy(id = id)
        for {
          updatedUser <- await(userService.update(draft.toUser)).right
        } yield UserFormat(updatedUser)
      }
    }
  }

  /**
    * Deletes user.
    */
  def delete(id: Long) = silhouette.SecuredAction(WithRole.admin).async { implicit request =>
    async {
      toResult {
        for {
          _ <- await(userService.delete(id)).right
        } yield NoContentResponse
      }
    }
  }
}
