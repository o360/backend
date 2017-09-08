package controllers.admin

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.EventJobService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions

import scala.concurrent.ExecutionContext

/**
  * Event job controller.
  */
@Singleton
class EventJobController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  eventJobService: EventJobService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  /**
    * Restarts failed job.
    */
  def restart(jobId: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    eventJobService
      .runFailedJob(jobId)
      .fold(
        toResult(_),
        _ => NoContent
      )
  }
}
