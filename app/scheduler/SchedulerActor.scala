package scheduler

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import services.EventJobService
import utils.{Config, Logger, TimestampConverter}

import scala.concurrent.ExecutionContext

/**
  * Scheduler actor.
  */
@Singleton
class SchedulerActor @Inject()(
  protected val config: Config,
  protected val eventJobService: EventJobService,
  implicit val ec: ExecutionContext
) extends Actor
  with Logger {

  private val maxAge = config.schedulerSettings.maxAgeMilliseconds

  def receive: Receive = {
    case SchedulerActor.Tick =>
      log.trace("scheduler tick")

      val now = TimestampConverter.now
      val from = new Timestamp(now.getTime - maxAge)

      eventJobService.get(from, now).map { jobs =>
        eventJobService.execute(jobs)
      }
  }
}

object SchedulerActor {
  case object Tick
}
