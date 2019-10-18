package io.tvc.convivial.twitter

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto.{deriveUnwrappedDecoder, deriveUnwrappedEncoder}

/**
  * Moving this out of the client because
  * it is referenced in the users package
  */
case class TwitterId(value: String) extends AnyVal

object TwitterId {
  implicit val decoder: Decoder[TwitterId] = deriveUnwrappedDecoder
  implicit val encoder: Encoder[TwitterId] = deriveUnwrappedEncoder
}
