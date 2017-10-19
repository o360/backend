package controllers.user

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.admin.PictureUploadHelper
import play.api.http.HttpEntity
import play.api.mvc.{ControllerComponents, ResponseHeader, Result}
import services.{FileService, UserService}
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext

/**
  * Profile picture controller.
  */
class ProfilePictureController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  val fileService: FileService,
  val userService: UserService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with PictureUploadHelper {

  val validExtensions = Set("jpg", "jpeg", "gif", "png")

  /**
    * Uploads and sets user profile picture.
    */
  def upload = silhouette.SecuredAction.async(parse.multipartFormData) { implicit request =>
    uploadInternal(request.identity.id, request.body)
      .fold(
        toResult(_),
        _ => NoContent
      )
  }

  /**
    * Returns user profile picture by user ID.
    */
  def get(userId: Long) = Action.async { _ =>
    userService
      .getById(userId)
      .fold(
        toResult(_),
        user => {
          user.pictureName
            .flatMap(fileService.get)
            .fold(Result(header = ResponseHeader(NOT_FOUND), body = HttpEntity.NoEntity))(file => Ok.sendFile(file))
        }
      )
  }
}
