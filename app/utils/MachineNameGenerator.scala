package utils

import java.util.UUID

/**
  * Entity machine name generator.
  */
object MachineNameGenerator {
  def generate: String = UUID.randomUUID().toString
}
