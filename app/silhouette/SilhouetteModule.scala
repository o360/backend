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
import scala.util.{Success, Try}

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
  def provideAuthenticatorService(crypter: Crypter,
                                  config: Config,
                                  ec: ExecutionContext): AuthenticatorService[JWTAuthenticator] = {

    val secret = config.cryptoSecret
    val jwtConfig = JWTAuthenticatorSettings(sharedSecret = secret)
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new JWTAuthenticatorService(jwtConfig, None, encoder, new SecureRandomIDGenerator()(ec), Clock())(ec)
  }

  @Provides
  def provideGoogleProvider(
    httpLayer: HTTPLayer,
    config: Config,
    ec: ExecutionContext
  ): CustomGoogleProvider = {

    val oauthConfig = OAuth2Settings(
      accessTokenURL = config.googleSettings.accessTokenUrl,
//      redirectURL = config.googleSettings.redirectUrl,
      clientID = config.googleSettings.clientId,
      clientSecret = config.googleSettings.clientSecret,
      scope = config.googleSettings.scope
    )

    new CustomGoogleProvider(
      httpLayer,
      new DefaultSocialStateHandler(Set(), new Signer {
        override def sign(data: String): String = data
        override def extract(message: String): Try[String] = Success(message)
      }),
      oauthConfig,
      ec
    )
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
