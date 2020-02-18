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

import play.api.Configuration

/**
  * Wrapper for application config.
  */
@Singleton
class Config @Inject() (protected val configuration: Configuration) {

  lazy val googleSettings: Config.OAuthGoogle = {
    val authConfig = configuration.get[Configuration]("auth")
    val accessTokenURL = authConfig.get[String]("silhouette.google.accessTokenURL")
    val redirectURL = authConfig.get[String]("silhouette.google.redirectURL")
    val clientID = authConfig.get[String]("silhouette.google.clientID")
    val clientSecret = authConfig.get[String]("silhouette.google.clientSecret")
    val scope = authConfig.getOptional[String]("silhouette.google.scope")

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

  lazy val userFilesPath: String = configuration.get[String]("userFilesPath")

  lazy val externalAuthServerUrl: Option[String] = configuration.getOptional[String]("auth.externalServerURL")
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
