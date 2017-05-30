package models.group

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
  hasChildren: Boolean
)
