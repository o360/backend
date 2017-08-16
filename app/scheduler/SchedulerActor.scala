package scheduler

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import play.api.libs.concurrent.Execution.Implicits._
import services.EventJobService
import utils.{Config, Logger, TimestampConverter}

/**
  * Scheduler actor.
  */
@Singleton
class SchedulerActor @Inject()(
  protected val config: Config,
  protected val eventJobService: EventJobService
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
