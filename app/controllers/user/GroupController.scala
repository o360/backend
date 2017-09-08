package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.group.ApiGroup
import controllers.authorization.AllowedStatus
import play.api.mvc.ControllerComponents
import services.GroupService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Group controller.
  */
@Singleton
class GroupController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  groupService: GroupService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'name)

  /**
    * Returns current user groups.
    */
  def getList =
    silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
      toResult(Ok) {
        groupService
          .listByUserId(request.identity.id)
          .map { groups =>
            Response.List(groups) { group =>
              ApiGroup(group)
            }(ListMeta.default)
          }
      }
    }
}
