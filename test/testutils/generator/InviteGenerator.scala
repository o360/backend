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

import java.time.LocalDateTime

import models.NamedEntity
import models.invite.Invite
import org.scalacheck.Arbitrary

/**
  * Invite generator for scalacheck.
  */
trait InviteGenerator extends TimeGenerator {

  implicit val inviteArb = Arbitrary {
    for {
      code <- Arbitrary.arbitrary[String]
      email <- Arbitrary.arbitrary[String]
      groupIds <- Arbitrary.arbitrary[Set[Long]]
      activationTime <- Arbitrary.arbitrary[Option[LocalDateTime]]
      creationTime <- Arbitrary.arbitrary[LocalDateTime]
    } yield Invite(code, email, groupIds.map(NamedEntity(_)), activationTime, creationTime)
  }
}
