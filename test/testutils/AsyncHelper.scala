package testutils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


/**
  * Helper for scala's future.
  */
trait AsyncHelper {

  /**
    * Waits for future competition and returns it's value.
    */
  def wait[T](future: Future[T]): T = Await.result(future, Duration(5, "second"))

  /**
    * Transforms given value to successful future.
    */
  def toFuture[T](value: T): Future[T] = Future.successful(value)

}
