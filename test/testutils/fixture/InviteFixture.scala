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
