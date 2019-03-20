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
