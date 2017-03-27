package dao

import com.typesafe.config.ConfigFactory
import org.scalatest.prop.Checkers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.PlaySpec
import org.flywaydb.core.Flyway

trait BaseDaoTest extends PlaySpec with GuiceOneAppPerSuite with Checkers {
  private val dbUrl = ConfigFactory.load.getString("slick.dbs.default.db.url")

  val flyway = new Flyway()
  flyway.setDataSource(dbUrl, null, null)
  flyway.clean()
  flyway.migrate()

}
