package services

import models.dao.fixture.FixtureSupport
import org.scalatest.OneInstancePerTest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import utils.AsyncHelper

/**
  * Base trait for service tests.
  */
trait BaseServiceTest
  extends PlaySpec
    with GeneratorDrivenPropertyChecks
    with AsyncHelper
    with MockitoSugar
    with FixtureSupport
    with OneInstancePerTest
