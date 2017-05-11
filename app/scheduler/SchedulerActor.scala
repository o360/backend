package scheduler

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import services.NotificationService
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
class SchedulerActor @Inject()(
  protected val configuration: Configuration,
  protected val notificationService: NotificationService
) extends Actor with Logger {

  private val interval = configuration.getMilliseconds("scheduler.interval").get
  private def now = new Timestamp(System.currentTimeMillis)
  private def from = new Timestamp(now.getTime - interval)

  def receive: Receive = {
    case SchedulerActor.Tick =>
      log.trace("scheduler tick")
      try {
        notificationService.sendEventsNotifications(from, now)
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
