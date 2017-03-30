package models.dao

import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.language.higherKinds


/**
  * Trait with helper methods for DAO's.
  */
trait DaoHelper {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  /**
    * Extensions for slick query.
    */
  implicit class QueryExtensions[E, U, C[_]](query: Query[E, U, C]) {
    /**
      * Filter query by given list of criteria.
      *
      * @param f               function returning list of criteria
      * @param allIfNoCriteria if true and no criteria specified, disable filtering, otherwise return empty result
      * @return slick query
      */
    def applyFilter(
      f: E => Seq[Option[Rep[Boolean]]],
      allIfNoCriteria: Boolean = true
    ): Query[E, U, C] = {
      query.filter { row =>
        val predicates = f(row).collect { case Some(p) => p }
        if (predicates.nonEmpty)
          predicates.reduceLeft(_ && _)
        else if (allIfNoCriteria)
          true: Rep[Boolean]
        else
          false: Rep[Boolean]
      }
    }
  }

}
