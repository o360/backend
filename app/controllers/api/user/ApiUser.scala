/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.api.user

import java.time.{DateTimeException, ZoneId}

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.user.User
import models.user.User.Status
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import io.scalaland.chimney.dsl._
import scalaz.std.option._

/**
  * User format model.
  */
case class ApiUser(
  id: Long,
  firstName: Option[String],
  lastName: Option[String],
  email: Option[String],
  gender: Option[ApiUser.ApiGender],
  role: ApiUser.ApiRole,
  status: ApiUser.ApiStatus,
  timezone: ZoneId,
  termsApproved: Boolean,
  hasPicture: Boolean
) extends Response {

  def toModel: User =
    this
      .into[User]
      .withFieldComputed(_.gender, _.gender.map(_.value))
      .withFieldComputed(_.role, _.role.value)
      .withFieldComputed(_.status, _.status.value)
      .withFieldConst(_.pictureName, none[String])
      .transform
}

object ApiUser {

  private implicit val zoneIdFormat = new Format[ZoneId] {
    def reads(json: JsValue): JsResult[ZoneId] = json match {
      case JsString(str) =>
        try {
          JsSuccess(ZoneId.of(str))
        } catch {
          case e: DateTimeException => JsError(e.getMessage)
        }
      case _ => JsError("string expected")
    }
    def writes(o: ZoneId): JsValue = JsString(o.getId)
  }

  private val reads: Reads[ApiUser] = (
    (__ \ "id").read[Long] and
      (__ \ "firstName").readNullable[String](maxLength[String](1024)) and
      (__ \ "lastName").readNullable[String](maxLength[String](1024)) and
      (__ \ "email").readNullable[String](maxLength[String](255)) and
      (__ \ "gender").readNullable[ApiUser.ApiGender] and
      (__ \ "role").read[ApiUser.ApiRole] and
      (__ \ "status").read[ApiUser.ApiStatus] and
      (__ \ "timezone").read[ZoneId] and
      (__ \ "termsApproved").read[Boolean]
  )(ApiUser(_, _, _, _, _, _, _, _, _, false))

  implicit val format = Format(reads, Json.writes[ApiUser])

  /**
    * Converts user to user response.
    *
    * @param user user
    * @return user response
    */
  def apply(user: User): ApiUser =
    user
      .into[ApiUser]
      .withFieldComputed(_.gender, _.gender.map(ApiGender(_)))
      .withFieldComputed(_.role, x => ApiRole(x.role))
      .withFieldComputed(_.status, x => ApiStatus(x.status))
      .withFieldComputed(_.hasPicture, _.pictureName.nonEmpty)
      .transform

  /**
    * Format for user role.
    */
  case class ApiRole(value: User.Role) extends EnumFormat[User.Role]
  object ApiRole extends EnumFormatHelper[User.Role, ApiRole]("role") {

    override protected val mapping: Map[String, User.Role] = Map(
      "user" -> User.Role.User,
      "admin" -> User.Role.Admin
    )
  }

  /**
    * Format for user status.
    */
  case class ApiStatus(value: User.Status) extends EnumFormat[User.Status]
  object ApiStatus extends EnumFormatHelper[User.Status, ApiStatus]("status") {

    override protected val mapping: Map[String, Status] = Map(
      "new" -> User.Status.New,
      "approved" -> User.Status.Approved
    )
  }

  /**
    * Format for user gender.
    */
  case class ApiGender(value: User.Gender) extends EnumFormat[User.Gender]
  object ApiGender extends EnumFormatHelper[User.Gender, ApiGender]("gender") {

    override protected def mapping: Map[String, User.Gender] = Map(
      "male" -> User.Gender.Male,
      "female" -> User.Gender.Female
    )
  }
}
