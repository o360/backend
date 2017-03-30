package utils

import play.api.{Logger => PlayLogger}

/**
  * Application logger.
  */
trait Logger {

  val log = PlayLogger("sop")

}
