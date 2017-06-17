package models.group

import models.NamedEntity

/**
  * Group.
  *
  * @param id          DB ID
  * @param parentId    parent ID
  * @param name        name
  * @param hasChildren true if group has children
  */
case class Group(
  id: Long,
  parentId: Option[Long],
  name: String,
  hasChildren: Boolean,
  level: Int
) {
  def toNamedEntity = NamedEntity(id, name)
}

object Group {
  val namePlural = "groups"
  val nameSingular = "group"
}
