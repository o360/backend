package controllers.api

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.libs.json._

import scala.util.control.NonFatal

/**
  * Format for timestamp.
  */
object TimestampFormat {

  private val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss'Z'"

  private val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern)
  private val parse = LocalDateTime.parse(_: CharSequence, dateTimeFormatter)

  implicit val timestampFormat = new Format[Timestamp] {
    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(str) =>
        try {
          JsSuccess(Timestamp.valueOf(parse(str)))
        } catch {
          case NonFatal(_) => JsError(s"Wrong datetime. Expected format: yyyy-MM-ddTHH:mm:ssZ")
        }
      case _ => JsError("Expected string")
    }
    def writes(o: Timestamp): JsValue = JsString(o.toLocalDateTime.format(dateTimeFormatter))
  }
}
