package io.tvc.convivial.profile

import enumeratum._
import io.circe.generic.JsonCodec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import io.tvc.convivial.storage.PgEnum

import scala.collection.immutable

/**
  * This is very much a sketch of the future
  * in that I doubt cities and interests can sensibly be hard coded
  */
sealed trait Profile

object Profile {

  implicit val config: Configuration = Configuration.default.withDiscriminator("status")
  implicit val decoder: Decoder[Profile] = deriveConfiguredDecoder
  implicit val encoder: Encoder[Profile] = deriveConfiguredEncoder

  @JsonCodec
  case class Complete(location: City, interests: List[Interest]) extends Profile
  case object Incomplete extends Profile

  sealed trait City extends EnumEntry
  object City extends Enum[City] with CirceEnum[City] with DoobieEnum[City] with PgEnum[City] {
    val values: immutable.IndexedSeq[City] = findValues
    case object London extends City
    case object Paris extends City
  }

  sealed trait Interest extends EnumEntry
  object Interest extends Enum[Interest] with CirceEnum[Interest] with DoobieEnum[Interest] with PgEnum[Interest] {
    val values: immutable.IndexedSeq[Interest] = findValues
    case object Technology extends Interest
    case object History extends Interest
    case object Wine extends Interest
  }
}
