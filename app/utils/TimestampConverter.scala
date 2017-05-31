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

  private val dateTimeApiFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
  private val dateTimePrettyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  /**
    * Converts API string to timestamp.
    *
    * @return either error if can't convert or timestamp.
    */
  def fromApiString(str: String): String \/ Timestamp = {
    try {
      val localDateTime = LocalDateTime.parse(str, dateTimeApiFormatter)
      new Timestamp(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli).right
    } catch {
      case NonFatal(_) => "Wrong datetime. Expected format: yyyy-MM-ddTHH:mm:ss".left
    }
  }

  /**
    * Returns API string timestamp representation.
    */
  def toApiString(timestamp: Timestamp): String = {
    LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime), ZoneOffset.UTC).format(dateTimeApiFormatter)
  }

  /**
    * Returns pretty printed timestamp in zone local time.
    */
  def toPrettyString(timestamp: Timestamp, zone: ZoneOffset): String = {
    LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime), zone).format(dateTimePrettyFormatter)
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
