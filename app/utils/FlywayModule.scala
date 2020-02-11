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
      val flyway = new Flyway()
      flyway.setLocations("migrations/common", "migrations/postgres")
      flyway.setDataSource(dbSettings.url, dbSettings.user, dbSettings.password)
      flyway.setOutOfOrder(true)
      flyway.migrate()
    } catch {
      case e: FlywayException => log.error("Can't apply migrations", e)
    }
  }
}
