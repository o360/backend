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

package silhouette

import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import services.UserService
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import com.mohiva.play.silhouette.api.crypto.{Crypter, CrypterAuthenticatorEncoder, Signer}
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.{Clock, HTTPLayer, PlayHTTPLayer}
import com.mohiva.play.silhouette.impl.authenticators.{JWTAuthenticator, JWTAuthenticatorService, JWTAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.util.SecureRandomIDGenerator
import play.api.libs.ws.WSClient
import utils.Config

import scala.concurrent.ExecutionContext
import com.mohiva.play.silhouette.impl.providers.oauth1.TwitterProvider
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.CookieSecretProvider
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.CookieSecretSettings

/**
  * DI module for silhouette.
  */
class SilhouetteModule extends AbstractModule {
  override def configure(): Unit = {
    bind(new TypeLiteral[Silhouette[DefaultEnv]]() {}).to(new TypeLiteral[SilhouetteProvider[DefaultEnv]]() {})
    bind(classOf[EventBus]).toInstance(EventBus())
  }

  @Provides
  def provideHttpLayer(client: WSClient, ec: ExecutionContext): HTTPLayer = new PlayHTTPLayer(client)(ec)

  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[JWTAuthenticator],
    eventBus: EventBus,
    ec: ExecutionContext
  ): Environment[DefaultEnv] = {
    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )(ec)
  }

  @Provides
  def provideAuthenticatorCrypter: Crypter = new DummyCrypter

  @Provides
  def provideSigner: Signer = new DummySigner

  @Provides
  def provideStateHandler(signer: Signer): SocialStateHandler = new DefaultSocialStateHandler(Set(), signer)

  @Provides
  def provideAuthenticatorService(
    crypter: Crypter,
    config: Config,
    ec: ExecutionContext
  ): AuthenticatorService[JWTAuthenticator] = {

    val secret = config.cryptoSecret
    val jwtConfig = JWTAuthenticatorSettings(sharedSecret = secret)
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new JWTAuthenticatorService(jwtConfig, None, encoder, new SecureRandomIDGenerator()(ec), Clock())(ec)
  }

  @Provides
  def provideGoogleProvider(
    httpLayer: HTTPLayer,
    stateHandler: SocialStateHandler,
    config: Config,
    ec: ExecutionContext
  ): Option[CustomGoogleProvider] =
    config.googleSettings.map { oauthSettings =>
      new CustomGoogleProvider(
        httpLayer,
        stateHandler,
        oauthSettings,
        ec
      )
    }

  @Provides
  def provideTwitterProvider(
    httpLayer: HTTPLayer,
    crypter: Crypter,
    signer: Signer,
    config: Config,
    ec: ExecutionContext
  ): Option[TwitterProvider] =
    config.twitterSettings.map { oauthSettings =>
      new TwitterProvider(
        httpLayer = httpLayer,
        service = new PlayOAuth1Service(oauthSettings),
        tokenSecretProvider = new CookieSecretProvider(
          CookieSecretSettings(),
          signer,
          crypter,
          Clock()
        ),
        settings = oauthSettings
      )
    }

  @Provides
  def provideFacebookProvider(
    httpLayer: HTTPLayer,
    stateHandler: SocialStateHandler,
    config: Config,
    ec: ExecutionContext
  ): Option[CustomFacebookProvider] =
    config.facebookSettings.map { oauthSettings =>
      new CustomFacebookProvider(
        httpLayer,
        stateHandler,
        oauthSettings,
        ec
      )
    }

  @Provides
  def provideVKProvider(
    httpLayer: HTTPLayer,
    stateHandler: SocialStateHandler,
    config: Config,
    ec: ExecutionContext
  ): Option[CustomVKProvider] =
    config.vkSettings.map { oauthSettings =>
      new CustomVKProvider(
        httpLayer,
        stateHandler,
        oauthSettings,
        ec
      )
    }

  @Provides
  def provideSocialProviderRegistry(
    googleProvider: Option[CustomGoogleProvider],
    twitterProvider: Option[TwitterProvider]
  ): SocialProviderRegistry =
    SocialProviderRegistry(
      Seq(
        googleProvider,
        twitterProvider
      ).flatten
    )

  @Provides
  def provideSecuredErrorHandler: SecuredErrorHandler = {
    new SilhouetteErrorHandler
  }
}
