package utils

import java.sql.Timestamp
import java.time.format.DateTimeFormatter

/**
  * Formatter for timestamp.
  */
object TimestampFormatter {

  private val dateTimePattern = "yyyy-MM-dd HH:mm"

  private val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern)

  /**
    * Converts timestamp to string.
    */
  def format(t: Timestamp) = t.toLocalDateTime.format(dateTimeFormatter)
}
