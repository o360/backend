package utils

import java.util.UUID

/**
  * Entity machine name generator.
  */
object RandomGenerator {
  private def guid = UUID.randomUUID().toString

  def generateMachineName: String = guid

  def generateInviteCode: String = guid

  def generateAnonymousUserName: String = guid

  def generateFilename: String = guid
}
