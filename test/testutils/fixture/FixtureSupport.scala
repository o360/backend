package testutils.fixture

import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.destination.Destination
import com.ninja_squad.dbsetup.operation.Operation

/**
  * Support for DB fixtures.
  */
trait FixtureSupport {
  private var fixtureOperations: Seq[Operation] = Seq()

  /**
    * Adds new operation.
    *
    * @param operation db setup operation
    */
  protected def addFixtureOperation(operation: => Operation): Unit = {
    fixtureOperations = fixtureOperations :+ operation
  }

  /**
    * Launch added fixture operations.
    *
    * @param destination where to apply operations.
    */
  def executeFixtureOperations(destination: Destination): Unit =
    fixtureOperations.foreach { operation =>
      val dbSetup = new DbSetup(destination, operation)
      dbSetup.launch()
    }
}
