package models.group

import models.NamedEntity

/**
  * Group.
  *
  * @param id       DB ID
  * @param parentId parent ID
  * @param name     name
  */
case class Group(
  id: Long,
  parentId: Option[Long],
  name: String
) {
  def toNamedEntity = NamedEntity(id, name)
}

object Group {
  val namePlural = "groups"
  val nameSingular = "group"
}
