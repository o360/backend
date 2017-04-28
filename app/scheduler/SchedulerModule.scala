package scheduler

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * Scheduler module.
  */
class SchedulerModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[SchedulerActor]("scheduler-actor")
    bind(classOf[Scheduler]).asEagerSingleton()
  }
}
