package controllers.admin

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import models.user.User
import play.api.libs.Files
import play.api.mvc.{ControllerComponents, MultipartFormData}
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
class ProfilePictureController @Inject() (
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
  def upload(userId: Long) = silhouette.SecuredAction.async(parse.multipartFormData) { implicit request =>
    uploadInternal(userId, request.body)
      .fold(
        toResult(_),
        _ => NoContent
      )
  }
}

trait PictureUploadHelper {
  val fileService: FileService
  val userService: UserService
  val validExtensions: Set[String]
  implicit val ec: ExecutionContext

  /**
    * Uploads and sets user profile picture.
    */
  def uploadInternal(userId: Long, body: MultipartFormData[Files.TemporaryFile])(implicit account: User) = {
    body
      .file("picture")
      .map { file =>
        for {
          user <- userService.getById(userId)
          filename <- EitherT.eitherT(fileService.upload(file.filename, file.ref.path, validExtensions).toFuture)
          _ <- userService.update(user.copy(pictureName = Some(filename)), updatePicture = true)
          _ = user.pictureName.foreach(fileService.delete)
        } yield ()
      }
      .getOrElse {
        val error: ApplicationError = BadRequestError.File.Request
        EitherT.eitherT(error.left.toFuture)
      }
  }
}
