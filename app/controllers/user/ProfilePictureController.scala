package controllers.user

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import play.api.http.HttpEntity
import play.api.mvc.{ControllerComponents, ResponseHeader, Result}
import services.{FileService, UserService}
import silhouette.DefaultEnv
import utils.errors.{ApplicationError, BadRequestError}
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext
import scalaz.EitherT
import scalaz.Scalaz.ToEitherOps

/**
  * Profile picture controller.
  */
class ProfilePictureController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  fileService: FileService,
  userService: UserService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController {

  private val validExtensions = Set("jpg", "jpeg", "gif", "png")

  /**
    * Uploads and sets user profile picture.
    */
  def upload = silhouette.SecuredAction.async(parse.multipartFormData) { implicit request =>
    request.body
      .file("picture")
      .map { file =>
        for {
          user <- userService.getById(request.identity.id)
          filename <- EitherT.eitherT(fileService.upload(file.filename, file.ref.path, validExtensions).toFuture)
          _ <- userService.update(user.copy(pictureName = Some(filename)), updatePicture = true)
          _ = user.pictureName.foreach(fileService.delete)
        } yield ()
      }
      .getOrElse {
        val error: ApplicationError = BadRequestError.File.Request
        EitherT.eitherT(error.left.toFuture)
      }
      .fold(
        toResult(_),
        _ => NoContent
      )
  }

  /**
    * Returns user profile picture by user ID.
    */
  def get(userId: Long) = silhouette.SecuredAction.async { implicit request =>
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
