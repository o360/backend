package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.authorization.AllowedRole
import services.EventJobService
import silhouette.DefaultEnv
import utils.listmeta.actions.ListActions
import utils.implicits.FutureLifting._

/**
  * Event job controller.
  */
@Singleton
class EventJobController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  eventJobService: EventJobService
) extends BaseController with ListActions {

  /**
    * Restarts failed job.
    */
  def restart(jobId: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    eventJobService.runFailedJob(jobId).fold(
      toResult(_),
      _ => NoContent
    )
  }
}
