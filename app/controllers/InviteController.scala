package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.Response
import controllers.api.invite.{ApiInvite, ApiInviteCode, ApiPartialInvite}
import controllers.authorization.AllowedRole
import services.InviteService
import silhouette.DefaultEnv
import utils.listmeta.actions.ListActions
import play.api.libs.concurrent.Execution.Implicits._
import utils.implicits.FutureLifting._

/**
  * Invite controller.
  */
@Singleton
class InviteController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  inviteService: InviteService
) extends BaseController
  with ListActions {

  /**
    * Returns list of invites.
    */
  def getList = silhouette.SecuredAction(AllowedRole.admin).andThen(ListAction).async { implicit request =>
    toResult(Ok) {
      inviteService.getList.map(invites =>
        Response.List(invites) { invite =>
          ApiInvite(invite)
      })
    }
  }

  /**
    * Bulk creates invites.
    */
  def bulkCreate = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[Seq[ApiPartialInvite]]) {
    implicit request =>
      inviteService
        .createInvites(request.body.map(_.toModel))
        .fold(
          toResult(_),
          _ => NoContent
        )
  }

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
