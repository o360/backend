package silhouette

import services.{User => UserService}
import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import com.mohiva.play.silhouette.api.crypto.{Crypter, CrypterAuthenticatorEncoder}
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.{Clock, HTTPLayer, PlayHTTPLayer}
import com.mohiva.play.silhouette.impl.authenticators.{JWTAuthenticator, JWTAuthenticatorService, JWTAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.providers.oauth2.GoogleProvider
import com.mohiva.play.silhouette.impl.providers.oauth2.state.DummyStateProvider
import com.mohiva.play.silhouette.impl.providers.{OAuth2Settings, OAuth2StateProvider, SocialProviderRegistry}
import com.mohiva.play.silhouette.impl.util.SecureRandomIDGenerator
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * DI module for silhouette.
  */
class SilhouetteModule extends AbstractModule {
  override def configure(): Unit = {
    bind(new TypeLiteral[Silhouette[DefaultEnv]](){}).to(new TypeLiteral[SilhouetteProvider[DefaultEnv]](){})
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
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    new DummyCrypter
  }
  @Provides
  def provideAuthenticatorService(
    crypter: Crypter,
    configuration: Configuration): AuthenticatorService[JWTAuthenticator] = {

    val secret = configuration.getString("play.crypto.secret").get
    val config = JWTAuthenticatorSettings(sharedSecret = secret)
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new JWTAuthenticatorService(config, None, encoder, new SecureRandomIDGenerator(), Clock())
  }

  @Provides
  def provideGoogleProvider(
    httpLayer: HTTPLayer,
    stateProvider: OAuth2StateProvider,
    configuration: Configuration
  ): GoogleProvider = {

    val oauthConfig = OAuth2Settings(
      accessTokenURL = configuration.getString("silhouette.google.accessTokenURL").get,
      redirectURL = configuration.getString("silhouette.google.redirectURL").get,
      clientID =configuration.getString("silhouette.google.clientID").get,
      clientSecret = configuration.getString("silhouette.google.clientSecret").get,
      scope = configuration.getString("silhouette.google.scope")
    )

    new GoogleProvider(httpLayer, stateProvider, oauthConfig)
  }

  @Provides
  def provideSocialProviderRegistry(googleProvider: GoogleProvider): SocialProviderRegistry = {

    SocialProviderRegistry(Seq(googleProvider))
  }

  @Provides
  def provideSecuredErrorHandler: SecuredErrorHandler = {
    new SilhouetteErrorHandler
  }
}
