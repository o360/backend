package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.invite.{ApiInvite, ApiInviteCode, ApiPartialInvite}
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.InviteService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

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
