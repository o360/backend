package models.dao.fixture

import com.ninja_squad.dbsetup.operation.Insert
import com.ninja_squad.dbsetup.operation.Insert.Builder

/**
  * Helper for fixtures building.
  */
trait FixtureHelper {

  /**
    * Extensions for db setup builder.
    */
  implicit class BuilderExtensions(builder: Insert.Builder) {

    /**
      * Add rows of values to insert using scala's varargs.
      */
    def scalaValues(params: Any*): Builder = builder.values(params.map(_.asInstanceOf[AnyRef]): _*)

  }

}
