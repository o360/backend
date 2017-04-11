package controllers.api

import play.api.libs.json._
import play.api.mvc.QueryStringBindable

/**
  * Base format for enumerations.
  */
trait EnumFormat[A] extends Response {
  /**
    * Inner value.
    */
  def value: A
}
/**
  * Helper for base enumeration format. Includes Json reads and writes, query binder.
  * Serializes enums to strings and backward based on mapping.
  *
  * @param name enumeration name
  * @tparam A inner enumeration type
  * @tparam B enumeration type
  */
abstract class EnumFormatHelper[A, B <: EnumFormat[A]](val name: String) {

  /**
    * Mapping between string representation and concrete enum values.
    */
  protected def mapping: Map[String, A]

  /**
    * Create enum format from inner value.
    *
    * @param value inner format value
    * @return created format
    */
  protected def apply(value: A): B


  /**
    * Reads string and returns either inner value or error message.
    */
  private def read(input: String): Either[String, A] = mapping.get(input) match {
    case None =>
      Left(s"Unable to parse $name. Unknown value [$input]. " +
        s"Possible values: [${mapping.keys.mkString(", ")}]")
    case Some(value) => Right(value)
  }

  /**
    * Converts inner enum value to string.
    */
  private def write(value: A): String = {
    mapping.find(_._2 == value).get._1
  }

  /**
    * Play json writes.
    */
  implicit def writes = new Writes[B] {
    override def writes(o: B): JsValue = {
      JsString(write(o.value))
    }
  }

  /**
    * Play json reads.
    */
  implicit def reads = new Reads[B] {
    override def reads(json: JsValue): JsResult[B] = json match {
      case JsString(statusStr) =>
        read(statusStr) match {
          case Left(error) => JsError(error)
          case Right(value) => JsSuccess(apply(value))
        }
      case _ => JsError("Expected string")
    }
  }

  /**
    * Play's query string binder. Adds ability to use enum as query string parameter in routes.
    * It is necessary to add package to routes import in build.sbt to make it work.
    */
  implicit def queryBinder(implicit stringBinder: QueryStringBindable[String]) =
    new QueryStringBindable[B] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, B]] = {
        stringBinder.bind(key, params) match {
          case None => None
          case Some(Left(error)) => Some(Left(error))
          case Some(Right(str)) =>
            read(str) match {
              case Left(error) => Some(Left(error))
              case Right(value) => Some(Right(apply(value)))
            }
        }
      }
      override def unbind(key: String, value: B): String = {
        stringBinder.unbind(key, write(value.value))
      }
    }
}
