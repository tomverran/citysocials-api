package io.tvc.convivial.users

import io.circe.generic.JsonCodec
import io.circe.generic.extras.semiauto.{deriveUnwrappedDecoder, deriveUnwrappedEncoder}
import io.circe.{Decoder, Encoder}
import io.tvc.convivial.twitter.TwitterId

/**
  * Currently very anemic model of a user
  * though I think things like location etc should live in a profile
  * rather than directly on this record so you can exist without a profile setup
  */
@JsonCodec
case class User(
  name: String,
  twitterId: TwitterId
)

object User {

  /**
    * The id of the user we actually own
    */
  case class Id(value: Int) extends AnyVal

  object Id {
    implicit val decoder: Decoder[Id] = deriveUnwrappedDecoder
    implicit val encoder: Encoder[Id] = deriveUnwrappedEncoder
  }
}
