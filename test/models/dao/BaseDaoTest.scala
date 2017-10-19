package models.dao

import com.ninja_squad.dbsetup.destination.DriverManagerDestination
import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import testutils.AsyncHelper
import testutils.fixture.FixtureSupport

import scala.reflect.ClassTag

/**
  * Base trait for DAO tests.
  */
trait BaseDaoTest
  extends PlaySpec
  with GuiceOneAppPerSuite
  with GeneratorDrivenPropertyChecks
  with AsyncHelper
  with FixtureSupport
  with BeforeAndAfterEach {

  private val dbUrl = ConfigFactory.load.getString("slick.dbs.default.db.url")
  private val dbUser = ConfigFactory.load.getString("slick.dbs.default.db.user")
  private val dbPass = ConfigFactory.load.getString("slick.dbs.default.db.password")

  private val flyway = new Flyway()

  private val dbSetupDestination = new DriverManagerDestination(dbUrl, dbUser, dbPass)

  override def beforeEach(): Unit = {
    flyway.setLocations("migrations/common", "migrations/h2")
    flyway.setDataSource(dbUrl, dbUser, dbPass)
    flyway.clean()
    flyway.migrate()
    executeFixtureOperations(dbSetupDestination)
  }

  /**
    * Returns instance of given class with all dependencies injected.
    */
  protected def inject[T: ClassTag]: T = app.injector.instanceOf[T]

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 6, sizeRange = 15)
}
