/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.api.export

import java.time.ZoneOffset

import controllers.api.Response
import controllers.api.event.ApiEvent
import models.event.Event
import play.api.libs.json.Json

/**
  * API event export model.
  */
case class ApiShortEvent(
  id: Long,
  description: Option[String],
  start: Long,
  end: Long,
  status: ApiEvent.EventStatus
) extends Response

object ApiShortEvent {
  implicit val writes = Json.writes[ApiShortEvent]

  def apply(e: Event): ApiShortEvent = ApiShortEvent(
    e.id,
    e.description,
    e.start.atZone(ZoneOffset.UTC).toEpochSecond,
    e.end.atZone(ZoneOffset.UTC).toEpochSecond,
    ApiEvent.EventStatus(e.status)
  )
}
