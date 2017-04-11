package services

import models.ListWithTotal
import utils.errors.ApplicationError

import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * Service methods results.
  */
trait ServiceResults[A] {

  type SingleResult = Future[Either[ApplicationError, A]]

  type ListResult = Future[Either[ApplicationError, ListWithTotal[A]]]

  type NoResult = Future[Either[ApplicationError, Unit]]

  /**
    * Converts single object to success result.
    */
  implicit def model2success(model: A): Either[ApplicationError, A] = Right(model)

  /**
    * Converts list of objects to success list result.
    */
  implicit def list2success(list: ListWithTotal[A]): Either[ApplicationError, ListWithTotal[A]] = Right(list)

  /**
    * Converts error to error result.
    */
  implicit def error2error[R](error: ApplicationError): Either[ApplicationError, R] = Left(error)

  /**
    * Use in methods where no result required.
    */
  def unitResult: Either[ApplicationError, Unit] = Right(())

}
