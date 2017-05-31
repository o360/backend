package controllers.api

import java.sql.Timestamp

import play.api.libs.json._
import utils.TimestampConverter

/**
  * Api format for timestamp.
  */
object ApiTimestamp {

  implicit val timestampFormat = new Format[Timestamp] {
    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(str) =>
        TimestampConverter.fromApiString(str).fold(
          JsError(_),
          JsSuccess(_)
        )
      case _ => JsError("Expected string")
    }
    def writes(o: Timestamp): JsValue = JsString(TimestampConverter.toApiString(o))
  }
}
