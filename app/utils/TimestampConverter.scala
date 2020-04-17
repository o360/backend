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

package utils

import java.time._
import java.time.format.DateTimeFormatter

/**
  * Converter for date and time.
  */
object TimestampConverter {

  private val dateTimePrettyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  /**
    * Returns pretty printed timestamp in zone local time.
    */
  def toPrettyString(dateTime: LocalDateTime, zone: ZoneId): String = {
    val dateTimeString = fromUtc(dateTime, zone).format(dateTimePrettyFormatter)
    val zoneOffset = zone.getId
    s"$dateTimeString ($zoneOffset)"
  }

  /**
    * Converts dateTime from UTC to local zone.
    */
  def fromUtc(dateTime: LocalDateTime, zone: ZoneId): LocalDateTime = {
    val zonedDateTime = ZonedDateTime.of(dateTime, ZoneOffset.UTC)
    val dateTimeAtTargetZone = zonedDateTime.withZoneSameInstant(zone)
    dateTimeAtTargetZone.toLocalDateTime
  }

  /**
    * Converts local zone dateTime to UTC.
    */
  def toUtc(dateTime: LocalDateTime, zone: ZoneId): LocalDateTime = {
    val zonedSourceTime = ZonedDateTime.of(dateTime, zone)
    val dateTimeAtUtc = zonedSourceTime.withZoneSameInstant(ZoneOffset.UTC)
    dateTimeAtUtc.toLocalDateTime
  }

  /**
    * Returns current UTC time.
    */
  def now: LocalDateTime = {
    ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime
  }
}
