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

package models.notification

/**
  * Notification kind.
  */
sealed trait NotificationKind

/**
  * Notification for soon event starting.
  */
case object PreBegin extends NotificationKind

/**
  * Notification for event start.
  */
case object Begin extends NotificationKind

/**
  * Notification for soon event ending.
  */
case object PreEnd extends NotificationKind

/**
  * Notification for event ending.
  */
case object End extends NotificationKind

/**
  * Kind of notification recipient.
  */
sealed trait NotificationRecipient

/**
  * Notification for project auditor.
  */
case object Auditor extends NotificationRecipient

/**
  * Notification for respondent.
  */
case object Respondent extends NotificationRecipient
