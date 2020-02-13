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

import java.time.temporal.ChronoUnit

import akka.actor.Actor
import javax.inject.{Inject, Singleton}
import services.event.EventJobService
import utils.{Config, Logger, TimestampConverter}

import scala.concurrent.ExecutionContext

/**
  * Scheduler actor.
  */
@Singleton
class SchedulerActor @Inject() (
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
      val from = now.minus(maxAge, ChronoUnit.MILLIS)

      eventJobService.get(from, now).map { jobs =>
        eventJobService.execute(jobs)
      }
  }
}

object SchedulerActor {
  case object Tick
}
