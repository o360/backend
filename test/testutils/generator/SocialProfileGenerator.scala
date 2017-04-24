package testutils.generator

import com.mohiva.play.silhouette.api.LoginInfo
import models.user.User
import org.scalacheck.Arbitrary
import silhouette.CustomSocialProfile

/**
  * Social profile generator for scalacheck.
  */
trait SocialProfileGenerator extends UserGenerator {

  implicit val socialProfileArbitrary = Arbitrary {
    for {
      provId <- Arbitrary.arbitrary[String]
      provKey <- Arbitrary.arbitrary[String]
      fullName <- Arbitrary.arbitrary[Option[String]]
      email <- Arbitrary.arbitrary[Option[String]]
      gender <- Arbitrary.arbitrary[Option[User.Gender]]
    } yield CustomSocialProfile(LoginInfo(provId, provKey), fullName, email, gender)
  }

}
