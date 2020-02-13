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
    val withIndex = s.zipWithIndex.toMap.view.mapValues(_.toByte).toMap
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
