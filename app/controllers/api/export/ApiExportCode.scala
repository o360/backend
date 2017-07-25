package controllers.api.export

import play.api.libs.json.Json

/**
  * API model for code required for export.
  */
case class ApiExportCode(
  code: String
)

object ApiExportCode {
  implicit val reads = Json.reads[ApiExportCode]
}
