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
    val dateTimeString = dateTime.format(dateTimePrettyFormatter)
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
