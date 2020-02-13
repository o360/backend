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

package testutils.generator

import models.notification._
import org.scalacheck.{Arbitrary, Gen}

/**
  * Notification generator for scalacheck.
  */
trait NotificationGenerator {

  implicit val notificationKindArb = Arbitrary[NotificationKind] {
    Gen.oneOf(PreBegin, Begin, PreEnd, End)
  }

  implicit val notificationRecipientArb = Arbitrary[NotificationRecipient] {
    Gen.oneOf(Respondent, Auditor)
  }
}
