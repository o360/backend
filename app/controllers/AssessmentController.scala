package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.Response
import controllers.api.assessment.ApiAssessment
import controllers.authorization.AllowedStatus
import services.AssessmentService
import silhouette.DefaultEnv
import utils.listmeta.actions.ListActions
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

/**
  * Asssessment controller.
  */
@Singleton
class AssessmentController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  assessmentService: AssessmentService
) extends BaseController with ListActions {

  /**
    * Returns list of available assessmnet objects.
    */
  def getList(eventId: Long, projectId: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
      toResult(Ok) {
        assessmentService
          .getList(eventId, projectId)
          .map { assessmentObjects =>
            Response.List(assessmentObjects)(ApiAssessment(_))(ListMeta.default)
          }
      }
    }
}
