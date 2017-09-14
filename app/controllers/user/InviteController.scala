package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.invite.ApiInviteCode
import play.api.mvc.ControllerComponents
import services.InviteService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext

/**
  * Invite controller.
  */
@Singleton
class InviteController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  inviteService: InviteService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController {

  /**
    * Submits invite code.
    */
  def submit = silhouette.SecuredAction.async(parse.json[ApiInviteCode]) { implicit request =>
    inviteService
      .applyInvite(request.body.code)
      .fold(
        toResult(_),
        _ => NoContent
      )
  }
}
