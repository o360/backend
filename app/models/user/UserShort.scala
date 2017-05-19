package models.user

/**
  * Short user model.
  */
case class UserShort(
  id: Long,
  name: String,
  gender: User.Gender
)

object UserShort {
  def fromUser(user: User) = UserShort(
    user.id,
    user.name.getOrElse(""),
    user.gender.getOrElse(User.Gender.Male)
  )

  def apply(id: Long): UserShort = UserShort(id, "", User.Gender.Male)
}
