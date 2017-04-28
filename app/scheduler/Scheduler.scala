package scheduler

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration._


/**
  * Scheduler timer.
  */
class Scheduler @Inject()(
  protected val system: ActorSystem,
  @Named("scheduler-actor") protected val schedulerActor: ActorRef,
  protected val configuration: Configuration) {

  private val interval = configuration.getMilliseconds("scheduler.interval").get
  private val cancellable = system.scheduler.schedule(
    0.milliseconds,
    interval.milliseconds,
    schedulerActor,
    SchedulerActor.Tick
  )
}
