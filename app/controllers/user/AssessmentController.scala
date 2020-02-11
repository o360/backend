package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.assessment.{ApiAssessment, ApiPartialAssessment}
import controllers.authorization.AllowedStatus
import play.api.mvc.ControllerComponents
import services.AssessmentService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import utils.listmeta.actions.ListActions

import scala.concurrent.ExecutionContext

/**
  * Assessment controller.
  */
@Singleton
class AssessmentController @Inject() (
  silhouette: Silhouette[DefaultEnv],
  assessmentService: AssessmentService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  /**
    * Returns list of available assessment objects.
    */
  def getList(projectId: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      assessmentService
        .getList(projectId)
        .map { assessmentObjects =>
          Response.List(assessmentObjects)(ApiAssessment(_))(ListMeta.default)
        }
    }
  }

  /**
    * Submits form answers for given event and project ids.
    */
  def bulkSubmit(projectId: Long) =
    silhouette.SecuredAction(AllowedStatus.approved).async(parse.json[Seq[ApiPartialAssessment]]) { implicit request =>
      val model = request.body.map(_.toModel)
      assessmentService
        .bulkSubmit(projectId, model)
        .fold(
          error => toResult(error),
          _ => NoContent
        )
    }
}
