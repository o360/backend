package models.dao

import models.EntityKind
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.reflect.ClassTag

/**
  * DTO to DB type mapper.
  */
trait EnumColumnMapper { _: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  /**
    * Automaps sequence of objects to corresponding byte values.
    */
  def mappedEnumSeq[A: ClassTag](s: A*) = {
    val withIndex = s.zipWithIndex.toMap.mapValues(_.toByte)
    MappedColumnType.base[A, Byte](
      withIndex,
      withIndex.map(_.swap)
    )
  }

  /**
    * Maps enum to anything else using mapping argument.
    */
  def mappedAny[A: ClassTag, DB: BaseColumnType](mapping: Map[DB, A]) = MappedColumnType.base[A, DB](
    mapping.map(_.swap),
    mapping
  )
}

/**
  * Entity kind column mapper.
  */
trait EntityKindColumnMapper extends EnumColumnMapper { _: HasDatabaseConfigProvider[JdbcProfile] =>

  implicit lazy val entityKindColumnMap = mappedEnumSeq[EntityKind](EntityKind.Template, EntityKind.Freezed)
}
