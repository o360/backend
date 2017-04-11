package models.dao

import models.ListWithTotal
import play.api.db.slick.HasDatabaseConfigProvider
import slick.ast.Ordering
import slick.driver.JdbcProfile
import slick.lifted.ColumnOrdered
import utils.listmeta.ListMeta
import utils.listmeta.pagination.Pagination
import utils.listmeta.pagination.Pagination.{WithPages, WithoutPages}
import utils.listmeta.sorting.Sorting

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
  implicit class QueryExtensions[E, U](query: Query[E, U, Seq]) {
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
    ): Query[E, U, Seq] = {
      query.filter {
        f(_)
          .collect {
            case Some(p) => p
          }
          .reduceLeftOption(_ && _)
          .getOrElse(allIfNoCriteria: Rep[Boolean])
      }
    }

    /**
      * Applies pagination to query.
      *
      * @param pagination pagination model
      * @return slick query
      */
    def applyPagination(pagination: Pagination): Query[E, U, Seq] = pagination match {
      case p@WithPages(size, _) =>
        query
          .drop(p.offset)
          .take(size)
      case WithoutPages =>
        query
    }

    /**
      * Sorts query by given sorting.
      *
      * @param mapping mapping between field names and slick columns
      */
    def applySorting(sorting: Sorting)(mapping: E => PartialFunction[Symbol, Rep[_]]): Query[E, U, Seq] = {
      def getSortedQuery(fields: Seq[Sorting.Field]): Query[E, U, Seq] = fields match {
        case Seq() => query
        case Sorting.Field(field, direction) +: tail =>
          getSortedQuery(tail).sortBy { x =>
            val pff = mapping(x)
            ColumnOrdered(
              column = mapping(x)(field),
              ord = direction match {
                case Sorting.Direction.Asc => Ordering().copy(direction = Ordering.Asc, nulls = Ordering.NullsFirst)
                case Sorting.Direction.Desc => Ordering().copy(direction = Ordering.Desc, nulls = Ordering.NullsLast)
              }
            )
          }
      }

      getSortedQuery(sorting.fields)
    }
  }

  /**
    * Returns list query result with total count.
    *
    * @param sortMapping mapping between field names and slick columns
    * @param meta        list meta
    * @return future of list result
    */
  def runListQuery[E, U](query: Query[E, U, Seq])
    (sortMapping: E => PartialFunction[Symbol, Rep[_]])
    (implicit meta: ListMeta): Future[ListWithTotal[U]] = async {
    val paginatedQuery = query
      .applySorting(meta.sorting)(sortMapping)
      .applyPagination(meta.pagination)

    val elements = await(db.run(paginatedQuery.result))
    val resultSize = elements.length

    meta.pagination match {
      case Pagination.WithoutPages =>
        ListWithTotal(resultSize, elements)
      case p@Pagination.WithPages(size, number) =>
        if (resultSize < size && resultSize > 0)
          ListWithTotal(p.offset + resultSize, elements)
        else {
          val total = await(db.run(query.length.result))
          ListWithTotal(total, elements)
        }
    }
  }

}
