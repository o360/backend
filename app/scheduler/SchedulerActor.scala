package scheduler

import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.user.User
import play.api.libs.concurrent.Execution.Implicits._
import utils.Logger
import utils.errors.ApplicationError

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}
import scalaz.{-\/, \/}

/**
  * Scheduler actor.
  */
@Singleton
class SchedulerActor @Inject()() extends Actor with Logger {

  private implicit val account = User.admin

  def receive: Receive = {
    case SchedulerActor.Tick =>
      log.trace("scheduler tick")
      try {
        // Do smth
      } catch {
        case NonFatal(e) =>
          log.error("scheduler", e)
      }
  }

  /**
    * Logs error if future failed.
    */
  private def logError(result: Future[ApplicationError \/ _]): Unit = {
    result.onComplete {
      case Success(-\/(error)) =>
        log.error(s"scheduler [${error.getCode}] ${error.getMessage}; ${error.getLogMessage}")
      case Failure(e) =>
        log.error("scheduler", e)
      case _ =>
    }
  }
}

object SchedulerActor {
  case object Tick
}
