package utils

import javax.inject.{Inject, Singleton}

import play.api.Configuration

/**
  * Wrapper for application config.
  */
@Singleton
class Config @Inject()(protected val configuration: Configuration) {

  lazy val googleSettings: Config.OAuthGoogle = {
    val accessTokenURL = configuration.getString("silhouette.google.accessTokenURL").get
    val redirectURL = configuration.getString("silhouette.google.redirectURL").get
    val clientID = configuration.getString("silhouette.google.clientID").get
    val clientSecret = configuration.getString("silhouette.google.clientSecret").get
    val scope = configuration.getString("silhouette.google.scope")

    Config.OAuthGoogle(accessTokenURL, redirectURL, clientID, clientSecret, scope)
  }

  lazy val schedulerSettings: Config.Scheduler = {
    val isEnabled = configuration.getBoolean("scheduler.enabled")
    val interval = configuration.getMilliseconds("scheduler.interval").get
    val maxAge = configuration.getMilliseconds("scheduler.max-age").get
    Config.Scheduler(isEnabled.contains(true), interval, maxAge)
  }

  lazy val mailerSettings: Config.Mailer = {
    val sendFromEmail = configuration.getString("play.mailer.from").get
    Config.Mailer(sendFromEmail)
  }

  lazy val cryptoSecret: String = configuration.getString("play.crypto.secret").get

  lazy val dbSettings: Config.DbSetting = {
    val url = configuration.getString("slick.dbs.default.db.url").get
    val user = configuration.getString("slick.dbs.default.db.user").get
    val password = configuration.getString("slick.dbs.default.db.password").get
    Config.DbSetting(url, user, password)
  }

  lazy val exportSecret: String = configuration.getString("export.secret").get
}

object Config {
  case class OAuthGoogle(
    accessTokenUrl: String,
    redirectUrl: String,
    clientId: String,
    clientSecret: String,
    scope: Option[String]
  )

  case class Scheduler(enabled: Boolean, intervalMilliseconds: Long, maxAgeMilliseconds: Long)

  case class Mailer(sendFrom: String)

  case class DbSetting(url: String, user: String, password: String)
}
