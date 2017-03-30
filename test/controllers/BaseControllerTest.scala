package controllers

import com.mohiva.play.silhouette.api.actions.{SecuredAction, UnsecuredAction, UserAwareAction}
import com.mohiva.play.silhouette.api.{Env, Environment, SilhouetteProvider}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import utils.AsyncHelper


/**
  * Base trait for controllers test.
  *
  */
trait BaseControllerTest
  extends PlaySpec
    with GeneratorDrivenPropertyChecks
    with GuiceOneAppPerSuite
    with AsyncHelper
    with MockitoSugar {


  /**
    * Returns silhouette with given environment.
    *
    * @param env environment
    */
  def getSilhouette[E <: Env](env: Environment[E]): SilhouetteProvider[E] = {
    new SilhouetteProvider(
      env,
      app.injector.instanceOf[SecuredAction],
      app.injector.instanceOf[UnsecuredAction],
      app.injector.instanceOf[UserAwareAction]
    )
  }
}
