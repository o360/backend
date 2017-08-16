package utils.listmeta.actions

import play.api.mvc.{Request, WrappedRequest}
import utils.listmeta.ListMeta

/**
  * Unsecured list request.
  *
  * @param meta  list meta info
  * @param inner inner request
  */
case class UnsecuredListRequest[B](
  meta: ListMeta,
  inner: Request[B]
) extends WrappedRequest[B](inner)
  with BaseListRequest
