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

package models.event

import java.time.LocalDateTime

/**
  * Job for event.
  */
trait EventJob {

  /**
    * Job ID.
    */
  def id: Long

  /**
    * Fire time.
    */
  def time: LocalDateTime

  /**
    * Id of event.
    */
  def eventId: Long

  /**
    * Job status.
    */
  def status: EventJob.Status

  /**
    * Similar to case class copy method.
    */
  def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob
}

object EventJob {

  /**
    * Job status.
    */
  trait Status
  object Status {

    /**
      * New job.
      */
    case object New extends Status

    /**
      * Job completed successfully.
      */
    case object Success extends Status

    /**
      * Job completed with failure.
      */
    case object Failure extends Status

    /**
      * Job cancelled.
      */
    case object Cancelled extends Status

    /**
      * Job is in progress.
      */
    case object InProgress extends Status
  }

  /**
    * Upload event report to google drive.
    */
  case class Upload(id: Long, eventId: Long, time: LocalDateTime, status: Status) extends EventJob {
    override def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob = {
      copy(id = id, eventId = eventId, status = status)
    }
  }

  /**
    * Send event notifications to email.
    */
  case class SendNotification(id: Long, eventId: Long, notification: Event.NotificationTime, status: Status)
    extends EventJob {
    override def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob = {
      copy(id = id, eventId = eventId, status = status)
    }
    override def time: LocalDateTime = notification.time
  }

  /**
    * Event start.
    */
  case class EventStart(id: Long, eventId: Long, time: LocalDateTime, status: Status) extends EventJob {
    override def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob = {
      copy(id = id, eventId = eventId, status = status)
    }
  }
}
