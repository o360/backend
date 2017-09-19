package models

/**
  * Kind of entity.
  */
sealed trait EntityKind

object EntityKind {

  /**
    * Template entity. Used in admin panel.
    */
  case object Template extends EntityKind

  /**
    * Freezed entity.
    */
  case object Freezed extends EntityKind
}
