package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.Response
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import controllers.api.template.{ApiPartialTemplate, ApiTemplate}
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.TemplateService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Template controller.
  */
@Singleton
class TemplateController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val templateService: TemplateService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'name, 'kind, 'recipient)

  /**
    * Returns list of templates with relations.
    */
  def getList(
    kind: Option[ApiNotificationKind],
    recipient: Option[ApiNotificationRecipient]
  ) =
    (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
      toResult(Ok) {
        templateService
          .getList(kind.map(_.value), recipient.map(_.value))
          .map { templates =>
            Response.List(templates)(ApiTemplate(_))
          }
      }
    }

  /**
    * Returns template with relations.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    toResult(Ok) {
      templateService
        .getById(id)
        .map(ApiTemplate(_))
    }
  }

  /**
    * Creates template.
    */
  def create = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialTemplate]) { implicit request =>
    toResult(Created) {
      val template = request.body.toModel()
      templateService
        .create(template)
        .map(ApiTemplate(_))
    }
  }

  /**
    * Updates template.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialTemplate]) {
    implicit request =>
      toResult(Ok) {
        val template = request.body.toModel(id)
        templateService
          .update(template)
          .map(ApiTemplate(_))
      }
  }

  /**
    * Removes template.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    templateService
      .delete(id)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }

  /**
    * Clones template.
    */
  def cloneTemplate(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    toResult(Created) {
      for {
        original <- templateService.getById(id)
        created <- templateService.create(original.copy(id = 0))
      } yield ApiTemplate(created)
    }
  }
}
