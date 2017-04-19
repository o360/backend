package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.Response
import controllers.api.form.ApiForm
import controllers.authorization.AllowedRole
import services.FormService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

/**
  * Form template controller.
  */
@Singleton
class FormController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val formService: FormService
) extends BaseController with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'name)

  /**
    * Returns list of form templates without elements.
    */
  def getList = (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
    toResult(Ok) {
      formService
        .getList()
        .map {
          forms =>
            Response.List(forms) {
              form => ApiForm(form)
            }
        }
    }
  }

  /**
    * Returns form template with elements.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    toResult(Ok) {
      formService
        .getById(id)
        .map(ApiForm(_))
    }
  }


  /**
    * Creates form template.
    */
  def create = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiForm]) { implicit request =>
    toResult(Created) {
      val form = request.body.toModel
      formService
        .create(form)
        .map(ApiForm(_))
    }
  }

  /**
    * Updates form template.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiForm]) { implicit request =>
    toResult(Ok) {
      val form = request.body.toModel.copy(id = id)
      formService
        .update(form)
        .map(ApiForm(_))
    }
  }

  /**
    * Removes form template.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    formService.delete(id).fold(
      error => toResult(error),
      _ => NoContent
    )
  }
}
