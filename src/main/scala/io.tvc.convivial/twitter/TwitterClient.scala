package io.tvc.convivial.twitter
import cats.effect.Sync
import cats.instances.option._
import cats.instances.string._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.generic.extras.Configuration.default
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import io.tvc.convivial.twitter.TwitterClient.{AccessToken, RequestToken, User, Verifier}
import org.http4s.Status.TemporaryRedirect
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.oauth1.{Consumer, Token, signRequest}
import org.http4s.headers.Location
import org.http4s.syntax.literals._

import scala.util.Try

/**
  * As minimal a twitter client as possible, just containing endpoints required
  * for a "sign in with twitter" flow - it turns out Twitter's API is a pain to work with
  */
trait TwitterClient[F[_]] {
  def requestToken: F[RequestToken]
  def redirect(requestToken: RequestToken): F[Response[F]]
  def accessToken(verifier: Verifier, requestToken: RequestToken): F[AccessToken]
  def verifyCredentials(accessToken: AccessToken): F[User]
}

object TwitterClient {

  val baseUri = uri"https://api.twitter.com"
  implicit val c: Configuration = default.withSnakeCaseMemberNames

  case class Config(
    consumer: Consumer,
    callback: Uri
  )

  case class RequestToken(token: Token, oauthCallbackConfirmed: Boolean)
  case class AccessToken(value: Token) extends AnyVal

  @ConfiguredJsonCodec
  case class User(
    name: String,
    screenName: String,
    email: Option[String]
  )

  case class Verifier(value: String) extends AnyVal

  def apply[F[_]](config: Config, client: Client[F])(implicit F: Sync[F]): TwitterClient[F] =
    new TwitterClient[F] {

      def ensureCallbackConfirmed(rt: RequestToken): F[Unit] =
        if (!rt.oauthCallbackConfirmed) F.raiseError(new Exception("callbackConfirmed false")) else F.unit

      def stringError(r: Response[F]): F[Throwable] =
        r.bodyAsText.compile.foldMonoid.map(j => new Exception(j))

      /**
        * The sign in with twitter flow begins by making an OAuth request
        * to fetch a request token, which infuriatingly is returned URL encoded, well done
        */
      def requestToken: F[RequestToken] =
        client.expectOr[UrlForm](
          signRequest(
            req = Request[F](uri = baseUri / "oauth" / "request_token"),
            consumer = config.consumer,
            callback = Some(config.callback),
            verifier = None,
            token = None
          )
        )(stringError)
          .flatMap { form =>
            Sync[F].fromEither(
              (
                form.get("oauth_token").headOption,
                form.get("oauth_token_secret").headOption,
                form.get("oauth_callback_confirmed").headOption.flatMap(c => Try(c.toBoolean).toOption)
              )
                .mapN { case (t, s, cc) => RequestToken(Token(t, s), cc) }
                .toRight(new Exception(s"Malformed response from twitter. Keys: ${form.values.keys}"))
            )
          }

      /**
        * Once the user has a request token
        * we need to redirect the user to twitter to sign in
        * and authorise the app to read their information
        */
      def redirect(rt: RequestToken): F[Response[F]] =
        ensureCallbackConfirmed(rt).as(
          Response(
            status = TemporaryRedirect,
            headers = Headers.of(
              Location(
                (baseUri / "oauth" / "authenticate").withQueryParam("oauth_token", rt.token.value)
              )
            )
          )
        )

      def accessToken(v: Verifier, rt: RequestToken): F[AccessToken] =
        client.expectOr[UrlForm](
          signRequest(
            req = Request[F](uri = baseUri / "oauth" / "access_token"),
            consumer = config.consumer,
            verifier = Some(v.value),
            token = Some(rt.token),
            callback = None,
          )
        )(stringError)
          .flatMap { form =>
            Sync[F].fromEither(
              (
                form.get("oauth_token").headOption,
                form.get("oauth_token_secret").headOption,
              )
                .mapN(Token.apply).map(AccessToken.apply)
                .toRight(new Exception(s"Malformed response from twitter. Keys: ${form.values.keys}"))
            )
          }

      def verifyCredentials(at: AccessToken): F[User] =
        client.expectOr[User](
          signRequest(
            req = Request[F](uri = baseUri / "1.1" / "account" / "verify_credentials.json"),
            consumer = config.consumer,
            token = Some(at.value),
            verifier = None,
            callback = None
          )
        )(stringError)
    }
}
