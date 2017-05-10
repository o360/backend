package models

/**
  * Entity with ID and name.
  */
class NamedEntity(
  val id: Long,
  val name: Option[String]
) {
  override def equals(obj: scala.Any): Boolean = {
    if (super.equals(obj)) true
    else if (obj == null) false
    else obj match {
      case that: NamedEntity => this.id == that.id
      case _ => false
    }
  }

  override def hashCode(): Int = id.hashCode
}

object NamedEntity {

  def apply(id: Long, name: Option[String]) = new NamedEntity(id, name)

  /**
    * Creates entity using ID.
    */
  def apply(id: Long): NamedEntity = new NamedEntity(id, None)

  /**
    * Creates entity using ID and name.
    */
  def apply(id: Long, name: String): NamedEntity = new NamedEntity(id, Some(name))
}
