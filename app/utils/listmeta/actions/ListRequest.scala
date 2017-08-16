package utils.listmeta.actions

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api.mvc.WrappedRequest
import silhouette.DefaultEnv
import utils.listmeta.ListMeta

/**
  * Secured list request.
  *
  * @param meta  list meta info
  * @param inner inner request
  */
case class ListRequest[B](
  meta: ListMeta,
  inner: SecuredRequest[DefaultEnv, B]
) extends WrappedRequest[B](inner)
  with BaseListRequest
