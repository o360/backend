package utils

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.TimeZone

/**
  * Converter for timestamps.
  */
object TimestampConverter {

  private val dateTimePrettyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  /**
    * Returns pretty printed timestamp in zone local time.
    */
  def toPrettyString(timestamp: Timestamp, zone: ZoneId): String = {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime), zone).format(dateTimePrettyFormatter)
    val zoneOffset = zone.getId
    s"$dateTime ($zoneOffset)"
  }

  /**
    * Converts timestamp from UTC to local zone.
    */
  def fromUtc(timestamp: Timestamp, zone: ZoneId): Timestamp = {
    val millis = TimeZone.getTimeZone(zone).getOffset(timestamp.getTime)
    new Timestamp(timestamp.getTime + millis)
  }

  /**
    * Converts local zone timestamp to UTC.
    */
  def toUtc(timestamp: Timestamp, zone: ZoneId): Timestamp = {
    val millis = TimeZone.getTimeZone(zone).getOffset(timestamp.getTime)
    new Timestamp(timestamp.getTime - millis)
  }

  /**
    * Returns current UTC time.
    */
  def now: Timestamp = {
    val instant = Instant.now()
    new Timestamp(instant.toEpochMilli)
  }
}
