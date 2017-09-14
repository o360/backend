package services

import models.ListWithTotal
import utils.errors.ApplicationError

import scala.concurrent.Future
import scalaz.EitherT

/**
  * Service methods results.
  */
trait ServiceResults[A] {

  /**
    * Future of either error or result.
    */
  type SingleResult = EitherT[Future, ApplicationError, A]

  /**
    * Future of either error or result list.
    */
  type ListResult = EitherT[Future, ApplicationError, ListWithTotal[A]]

  /**
    * Future of either error or unit.
    */
  type UnitResult = EitherT[Future, ApplicationError, Unit]

}
