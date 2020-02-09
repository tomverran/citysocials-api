package io.tvc.convivial

import io.tvc.convivial.twitter.TwitterId
import org.scalacheck.Gen

package object twitter {
  val twitterId: Gen[TwitterId] = Gen.alphaNumStr.map(TwitterId.apply)
}
