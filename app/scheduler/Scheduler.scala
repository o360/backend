package scheduler

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Environment
import utils.Config

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Scheduler timer.
  */
class Scheduler @Inject()(
  protected val system: ActorSystem,
  @Named("scheduler-actor") protected val schedulerActor: ActorRef,
  protected val config: Config,
  protected val environment: Environment,
  implicit val ec: ExecutionContext
) {

  if (config.schedulerSettings.enabled) {
    val interval = config.schedulerSettings.intervalMilliseconds
    system.scheduler.schedule(
      initialDelay = 30.seconds,
      interval = interval.milliseconds,
      receiver = schedulerActor,
      message = SchedulerActor.Tick
    )
  }
}
