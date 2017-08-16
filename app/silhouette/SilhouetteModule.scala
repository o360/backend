package silhouette

import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import services.UserService
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import com.mohiva.play.silhouette.api.crypto.{Crypter, CrypterAuthenticatorEncoder}
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.{Clock, HTTPLayer, PlayHTTPLayer}
import com.mohiva.play.silhouette.impl.authenticators.{JWTAuthenticator, JWTAuthenticatorService, JWTAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.providers.oauth2.state.DummyStateProvider
import com.mohiva.play.silhouette.impl.providers.{OAuth2Settings, OAuth2StateProvider, SocialProviderRegistry}
import com.mohiva.play.silhouette.impl.util.SecureRandomIDGenerator
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import utils.Config

/**
  * DI module for silhouette.
  */
class SilhouetteModule extends AbstractModule {
  override def configure(): Unit = {
    bind(new TypeLiteral[Silhouette[DefaultEnv]]() {}).to(new TypeLiteral[SilhouetteProvider[DefaultEnv]]() {})
    bind(classOf[EventBus]).toInstance(EventBus())
    bind(classOf[OAuth2StateProvider]).to(classOf[DummyStateProvider])
  }

  @Provides
  def provideHttpLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  @Provides
  def provideEnvironment(
    userService: UserService,
    authenticatorService: AuthenticatorService[JWTAuthenticator],
    eventBus: EventBus
  ): Environment[DefaultEnv] = {
    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  @Provides
  def provideAuthenticatorCrypter: Crypter = new DummyCrypter

  @Provides
  def provideAuthenticatorService(crypter: Crypter, config: Config): AuthenticatorService[JWTAuthenticator] = {

    val secret = config.cryptoSecret
    val jwtConfig = JWTAuthenticatorSettings(sharedSecret = secret)
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new JWTAuthenticatorService(jwtConfig, None, encoder, new SecureRandomIDGenerator(), Clock())
  }

  @Provides
  def provideGoogleProvider(
    httpLayer: HTTPLayer,
    stateProvider: OAuth2StateProvider,
    config: Config
  ): CustomGoogleProvider = {

    val oauthConfig = OAuth2Settings(
      accessTokenURL = config.googleSettings.accessTokenUrl,
      redirectURL = config.googleSettings.redirectUrl,
      clientID = config.googleSettings.clientId,
      clientSecret = config.googleSettings.clientSecret,
      scope = config.googleSettings.scope
    )

    new CustomGoogleProvider(httpLayer, stateProvider, oauthConfig)
  }

  @Provides
  def provideSocialProviderRegistry(googleProvider: CustomGoogleProvider): SocialProviderRegistry = {
    SocialProviderRegistry(Seq(googleProvider))
  }

  @Provides
  def provideSecuredErrorHandler: SecuredErrorHandler = {
    new SilhouetteErrorHandler
  }
}
