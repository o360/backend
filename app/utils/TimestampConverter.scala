package utils

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import scala.util.control.NonFatal
import scalaz.\/
import scalaz.Scalaz.ToEitherOps

/**
  * Converter for timestamps.
  */
object TimestampConverter {

  private val dateTimePrettyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  /**
    * Returns pretty printed timestamp in zone local time.
    */
  def toPrettyString(timestamp: Timestamp, zone: ZoneOffset): String = {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime), zone).format(dateTimePrettyFormatter)
    val zoneOffset = zone.getId
    s"$dateTime (UTC$zoneOffset)"
  }

  /**
    * Converts timestamp from UTC to local zone.
    */
  def fromUtc(timestamp: Timestamp, zone: ZoneOffset): Timestamp = {
    new Timestamp(timestamp.getTime + zone.getTotalSeconds * 1000)
  }

  /**
    * Converts local zone timestamp to UTC.
    */
  def toUtc(timestamp: Timestamp, zone: ZoneOffset): Timestamp = {
    new Timestamp(timestamp.getTime - zone.getTotalSeconds * 1000)
  }

  /**
    * Returns current UTC time.
    */
  def now: Timestamp = {
    val instant = Instant.now()
    new Timestamp(instant.toEpochMilli)
  }
}
