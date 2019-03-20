package models.dao
import java.sql.Timestamp
import java.time.LocalDateTime

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

/**
  * Java 8 time to sql classes mapper
  */
trait DateColumnMapper { _: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  implicit lazy val localDateToDate =
    MappedColumnType.base[LocalDateTime, Timestamp](Timestamp.valueOf, _.toLocalDateTime)
}
