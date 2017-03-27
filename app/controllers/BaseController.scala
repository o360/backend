package controllers

import play.api.libs.json.{JsValue, Writes}
import play.api.mvc.Controller


trait BaseController extends Controller{

  implicit class ResponseHelpers[T](any: T) {
    def toJson(implicit writes: Writes[T]): JsValue = {
      writes.writes(any)
    }
  }

}
