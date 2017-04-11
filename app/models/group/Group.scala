package models.group

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
)
