package controllers.api

import org.davidbild.tristate.Tristate
import play.api.mvc.QueryStringBindable

object TristateQueryBinder {

  /**
    * Query binder for tristate.
    * Unspecified if query param not specified.
    * Absent if query param equals 'null'
    * Present if query param is present and can be bind by inner binder.
    */
  implicit def tristateQueryBinder[A](
    implicit stringBinder: QueryStringBindable[String],
    innerBinder: QueryStringBindable[A]
  ): QueryStringBindable[Tristate[A]] = new QueryStringBindable[Tristate[A]] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Tristate[A]]] = {
      stringBinder.bind(key, params) match {
        case Some(Left(error)) => Some(Left(error))
        case None => Some(Right(Tristate.Unspecified))
        case Some(Right(str)) =>
          if (str == "null") {
            Some(Right(Tristate.Absent))
          } else {
            innerBinder.bind(key, params) match {
              case Some(Left(error)) => Some(Left(error))
              case None => Some(Right(Tristate.Unspecified))
              case Some(Right(inner)) => Some(Right(Tristate.present(inner)))
            }
          }
      }
    }
    override def unbind(key: String, value: Tristate[A]): String = {
      val str = value match {
        case Tristate.Present(inner) => inner.toString
        case Tristate.Absent => "null"
        case Tristate.Unspecified => ""
      }
      stringBinder.unbind(key, str)
    }
  }
}
