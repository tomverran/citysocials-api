package io.tvc.convivial

import io.tvc.convivial.profile.Profile.{City, Incomplete, Interest}
import org.scalacheck.Gen

package object profile {

  val city: Gen[City] = Gen.oneOf(City.values)
  val interest: Gen[Interest] = Gen.oneOf(Interest.values)

  val complete: Gen[Profile.Complete] =
    for {
      city <- city
      interests <- Gen.listOf(interest)
    } yield Profile.Complete(city, interests)

  val profile: Gen[Profile] =
    Gen.oneOf(complete, Gen.const(Incomplete))
}
