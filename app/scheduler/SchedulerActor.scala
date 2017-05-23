package scheduler

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import services.{NotificationService, UploadService}
import utils.Logger

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
  * Scheduler actor.
  */
@Singleton
class SchedulerActor @Inject()(
  protected val configuration: Configuration,
  protected val notificationService: NotificationService,
  protected val uploadService: UploadService
) extends Actor with Logger {

  private val interval = configuration.getMilliseconds("scheduler.interval").get

  def receive: Receive = {
    case SchedulerActor.Tick =>
      log.trace("scheduler tick")

      val now = new Timestamp(System.currentTimeMillis)
      val from = new Timestamp(now.getTime - interval)

      logFutureError(notificationService.sendEventsNotifications(from, now))

      logFutureError(uploadReports(from, now))
  }

  private def logFutureError(f: Future[_]) = f.onFailure {
    case NonFatal(e) => log.error("scheduler", e)
  }

  /**
    * Generates reports and uploads them to google drive.
    */
  private def uploadReports(from: Timestamp, to: Timestamp) = {
    for {
      uploadModels <- uploadService.getGroupedUploadModels(from, to)
      _ <- uploadService.upload(uploadModels)
    } yield ()
  }
}

object SchedulerActor {
  case object Tick
}
