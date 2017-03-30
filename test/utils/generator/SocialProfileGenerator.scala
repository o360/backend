package utils.generator

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import org.scalacheck.Arbitrary

/**
  * Social profile generator for scalacheck.
  */
trait SocialProfileGenerator {

  implicit val socialProfileArbitrary = Arbitrary {
    for {
      provId <- Arbitrary.arbitrary[String]
      provKey <- Arbitrary.arbitrary[String]
      firstName <- Arbitrary.arbitrary[Option[String]]
      lastName <- Arbitrary.arbitrary[Option[String]]
      fullName <- Arbitrary.arbitrary[Option[String]]
      email <- Arbitrary.arbitrary[Option[String]]
      avatarUrl <- Arbitrary.arbitrary[Option[String]]
    } yield CommonSocialProfile(LoginInfo(provId, provKey), firstName, lastName, fullName, email, avatarUrl)
  }

}
