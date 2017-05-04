package models

/**
  * Entity with ID and name.
  */
case class NamedEntity(
  id: Long,
  name: Option[String]
)

object NamedEntity {

  /**
    * Creates entity using ID.
    */
  def apply(id: Long): NamedEntity = NamedEntity(id, None)

  /**
    * Creates entity using ID and name.
    */
  def apply(id: Long, name: String): NamedEntity = NamedEntity(id, Some(name))
}
