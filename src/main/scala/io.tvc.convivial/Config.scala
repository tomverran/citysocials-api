package io.tvc.convivial

import cats.MonadError
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.data.Validated._
import io.tvc.convivial.twitter.TwitterClient
import cats.syntax.apply._
import org.http4s.Uri
import org.http4s.client.oauth1.Consumer


case class Config(
  twitter: TwitterClient.Config
)

object Config {

  private def str(name: String): ValidatedNel[String, String] =
    Validated.fromOption(sys.env.get(name), NonEmptyList.of(s"$name missing"))

  private def uri(name: String): ValidatedNel[String, Uri] =
    str(name).andThen(Uri.fromString(_).fold(e => invalidNel(s"$name: ${e.message}"), validNel))

  def load[F[_]](implicit F: MonadError[F, Throwable]): F[Config] =
    F.fromEither(
      (
        (
          str("TWITTER_CONSUMER_KEY"),
          str("TWITTER_CONSUMER_SECRET"),
        ).mapN(Consumer.apply),
        uri("TWITTER_CALLBACK")
      ).mapN(TwitterClient.Config.apply)
       .map(Config.apply)
       .leftMap(es => new Exception(s"Failed to load config:\n${es.toList.mkString("\n")}"))
       .toEither
    )
}
