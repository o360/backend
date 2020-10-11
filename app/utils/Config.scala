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
import com.mohiva.play.silhouette.impl.providers.OAuth2Settings
import com.mohiva.play.silhouette.impl.providers.OAuth1Settings

/**
  * Wrapper for application config.
  */
@Singleton
class Config @Inject() (protected val configuration: Configuration) {

  lazy val googleSettings: Option[OAuth2Settings] = loadOAuth2Settings("google")
  lazy val twitterSettings: Option[OAuth1Settings] = loadOAuth1Settings("twitter")
  lazy val facebookSettings: Option[OAuth2Settings] = loadOAuth2Settings("facebook")
  lazy val vkSettings: Option[OAuth2Settings] = loadOAuth2Settings("vk")

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

  lazy val usersAutoApprove: Boolean = configuration.get[Boolean]("usersAutoApprove")

  private def loadOAuth2Settings(provider: String): Option[OAuth2Settings] =
    for {
      providerConfig <- configuration.getOptional[Configuration](s"auth.silhouette.$provider")
      accessTokenURL <- providerConfig.getOptional[String]("accessTokenURL")
      redirectURL <- providerConfig.getOptional[String]("redirectURL")
      clientID <- providerConfig.getOptional[String]("clientID")
      clientSecret <- providerConfig.getOptional[String]("clientSecret")
    } yield OAuth2Settings(
      accessTokenURL = accessTokenURL,
      redirectURL = Some(redirectURL),
      clientID = clientID,
      clientSecret = clientSecret,
      apiURL = providerConfig.getOptional[String]("apiURL"),
      scope = providerConfig.getOptional[String]("scope")
    )

  private def loadOAuth1Settings(provider: String): Option[OAuth1Settings] =
    for {
      providerConfig <- configuration.getOptional[Configuration](s"auth.silhouette.$provider")
      requestTokenURL <- providerConfig.getOptional[String]("requestTokenURL")
      accessTokenURL <- providerConfig.getOptional[String]("accessTokenURL")
      authorizationURL <- providerConfig.getOptional[String]("authorizationURL")
      callbackURL <- providerConfig.getOptional[String]("callbackURL")
      consumerKey <- providerConfig.getOptional[String]("consumerKey")
      consumerSecret <- providerConfig.getOptional[String]("consumerSecret")
    } yield OAuth1Settings(
      requestTokenURL = requestTokenURL,
      accessTokenURL = accessTokenURL,
      authorizationURL = authorizationURL,
      callbackURL = callbackURL,
      consumerKey = consumerKey,
      consumerSecret = consumerSecret
    )

}

object Config {

  case class Scheduler(enabled: Boolean, intervalMilliseconds: Long, maxAgeMilliseconds: Long)

  case class Mailer(sendFrom: String)

  case class DbSetting(url: String, user: String, password: String)
}
