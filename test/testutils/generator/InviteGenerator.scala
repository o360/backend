package testutils.generator

import java.sql.Timestamp

import models.NamedEntity
import models.invite.Invite
import org.scalacheck.Arbitrary

/**
  * Invite generator for scalacheck.
  */
trait InviteGenerator extends TimestampGenerator {

  implicit val inviteArb = Arbitrary {
    for {
      code <- Arbitrary.arbitrary[String]
      email <- Arbitrary.arbitrary[String]
      groupIds <- Arbitrary.arbitrary[Set[Long]]
      activationTime <- Arbitrary.arbitrary[Option[Timestamp]]
      creationTime <- Arbitrary.arbitrary[Timestamp]
    } yield Invite(code, email, groupIds.map(NamedEntity(_)), activationTime, creationTime)
  }
}
