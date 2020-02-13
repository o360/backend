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

package testutils.fixture

import java.time.LocalDateTime

import com.ninja_squad.dbsetup.Operations._
import models.NamedEntity
import models.invite.Invite

/**
  * Invite model fixture.
  */
trait InviteFixture extends FixtureHelper with GroupFixture { self: FixtureSupport =>

  val Invites = Seq(
    Invite(
      code = "code",
      email = "email",
      groups = Set(NamedEntity(1, "1"), NamedEntity(2, "2")),
      activationTime = None,
      creationTime = LocalDateTime.of(2019, 1, 1, 0, 0)
    ),
    Invite(
      code = "code 2",
      email = "email 2",
      groups = Set(NamedEntity(4, "2-2"), NamedEntity(5, "2-2-1")),
      activationTime = Some(LocalDateTime.of(2019, 1, 2, 10, 27)),
      creationTime = LocalDateTime.of(2019, 1, 1, 0, 0)
    )
  )

  addFixtureOperation {
    sequenceOf(
      insertInto("invite")
        .columns("id", "code", "email", "activation_time", "creation_time")
        .scalaValues(1, "code", "email", null, Invites(0).creationTime)
        .scalaValues(2, "code 2", "email 2", Invites(1).activationTime.get, Invites(1).creationTime)
        .build,
      insertInto("invite_group")
        .columns("invite_id", "group_id")
        .scalaValues(1, 1)
        .scalaValues(1, 2)
        .scalaValues(2, 4)
        .scalaValues(2, 5)
        .build
    )
  }
}
