package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.group.ApiGroup
import controllers.authorization.AllowedStatus
import play.api.mvc.ControllerComponents
import services.{GroupService, UserService}
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import utils.listmeta.actions.ListActions

import scala.concurrent.ExecutionContext

/**
  * Group controller.
  */
@Singleton
class GroupController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  groupService: GroupService,
  userService: UserService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  /**
    * Returns current user groups.
    */
  def getList =
    silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
      getListByUserId(request.identity.id)(request)
    }

  def getListByUserId(userId: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      userService.getById(userId).flatMap { _ =>
        groupService
          .listByUserId(userId)
          .map { groups =>
            Response.List(groups) { group =>
              ApiGroup(group)
            }(ListMeta.default)
          }
      }
    }
  }
}
