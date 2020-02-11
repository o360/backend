package controllers.user

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.report.ApiReport
import controllers.authorization.AllowedStatus
import play.api.mvc.ControllerComponents
import services.ReportService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext

/**
  * Report controller.
  */
class ReportController @Inject() (
  protected val silhouette: Silhouette[DefaultEnv],
  protected val reportService: ReportService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController {

  def getAuditorReport(projectId: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      reportService
        .getAuditorReport(projectId)
        .map(_.map(ApiReport(_)))
        .map(Response.List(_))
    }
  }
}
