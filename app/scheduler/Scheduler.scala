/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
class Scheduler @Inject() (
  protected val system: ActorSystem,
  @Named("scheduler-actor") protected val schedulerActor: ActorRef,
  protected val config: Config,
  protected val environment: Environment,
  implicit val ec: ExecutionContext
) {

  if (config.schedulerSettings.enabled) {
    val interval = config.schedulerSettings.intervalMilliseconds
    system.scheduler.scheduleAtFixedRate(
      initialDelay = 30.seconds,
      interval = interval.milliseconds,
      receiver = schedulerActor,
      message = SchedulerActor.Tick
    )
  }
}
