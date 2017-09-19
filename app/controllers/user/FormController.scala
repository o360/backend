package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.form.ApiForm
import controllers.authorization.AllowedStatus
import play.api.mvc.ControllerComponents
import services.FormService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions

import scala.concurrent.ExecutionContext

/**
  * Form template controller.
  */
@Singleton
class FormController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val formService: FormService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  /**
    * Returns form template with elements for user.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      formService
        .getByIdWithAuth(id)
        .map(ApiForm(_))
    }
  }
}
