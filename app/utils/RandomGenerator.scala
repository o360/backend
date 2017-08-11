package utils

import java.util.UUID

/**
  * Entity machine name generator.
  */
object RandomGenerator {
  def generateMachineName: String = UUID.randomUUID().toString

  def generateInviteCode: String = UUID.randomUUID().toString

  def generateAnonymousUserName: String = UUID.randomUUID().toString
}
