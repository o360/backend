package utils

import javax.inject.{Inject, Singleton}

import play.api.Configuration

/**
  * Wrapper for application config.
  */
@Singleton
class Config @Inject()(protected val configuration: Configuration) {

  lazy val googleSettings: Config.OAuthGoogle = {
    val accessTokenURL = configuration.get[String]("silhouette.google.accessTokenURL")
    val redirectURL = configuration.get[String]("silhouette.google.redirectURL")
    val clientID = configuration.get[String]("silhouette.google.clientID")
    val clientSecret = configuration.get[String]("silhouette.google.clientSecret")
    val scope = configuration.getOptional[String]("silhouette.google.scope")

    Config.OAuthGoogle(accessTokenURL, redirectURL, clientID, clientSecret, scope)
  }

  lazy val schedulerSettings: Config.Scheduler = {
    val isEnabled = configuration.getOptional[Boolean]("scheduler.enabled")
    val interval = configuration.getMillis("scheduler.interval")
    val maxAge = configuration.getMillis("scheduler.max-age")
    Config.Scheduler(isEnabled.contains(true), interval, maxAge)
  }

  lazy val mailerSettings: Config.Mailer = {
    val sendFromEmail = configuration.get[String]("play.mailer.from")
    Config.Mailer(sendFromEmail)
  }

  lazy val cryptoSecret: String = configuration.get[String]("play.http.secret.key")

  lazy val dbSettings: Config.DbSetting = {
    val url = configuration.get[String]("slick.dbs.default.db.url")
    val user = configuration.get[String]("slick.dbs.default.db.user")
    val password = configuration.get[String]("slick.dbs.default.db.password")
    Config.DbSetting(url, user, password)
  }

  lazy val exportSecret: String = configuration.get[String]("export.secret")
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
