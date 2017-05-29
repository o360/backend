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
    Config.Scheduler(isEnabled.contains(true), interval)
  }

  lazy val mailerSettings: Config.Mailer = {
    val sendFromEmail = configuration.getString("play.mailer.from").get
    Config.Mailer(sendFromEmail)
  }

  lazy val cryptoSecret: String = configuration.getString("play.crypto.secret").get
}

object Config {
  case class OAuthGoogle(
    accessTokenUrl: String,
    redirectUrl: String,
    clientId: String,
    clientSecret: String,
    scope: Option[String]
  )

  case class Scheduler(enabled: Boolean, intervalMilliseconds: Long)

  case class Mailer(sendFrom: String)
}
