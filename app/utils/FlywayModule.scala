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

package utils

import javax.inject.{Inject, Singleton}

import com.google.inject.AbstractModule
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import play.api.{Environment, Mode}

/**
  * Flyway DI module.
  */
class FlywayModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[FlywayInitializer]).asEagerSingleton()
  }
}

/**
  * Automatically applies migrations when application is in prod mode.
  */
@Singleton
class FlywayInitializer @Inject() (
  protected val config: Config,
  protected val environment: Environment
) extends Logger {

  if (environment.mode == Mode.Prod) {
    try {
      val dbSettings = config.dbSettings
      val flyway = Flyway
        .configure()
        .locations(
          "migrations/common",
          "migrations/postgres"
        )
        .dataSource(
          dbSettings.url,
          dbSettings.user,
          dbSettings.password
        )
        .table("schema_version")
        .outOfOrder(true)
        .load()

      flyway.migrate()
    } catch {
      case e: FlywayException => log.error("Can't apply migrations", e)
    }
  }
}
