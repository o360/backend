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

import com.ninja_squad.dbsetup.destination.DriverManagerDestination
import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
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
  with ScalaCheckDrivenPropertyChecks
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
