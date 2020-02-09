package io.tvc.convivial.twitter

import cats.effect.concurrent.Ref
import cats.effect.{IO, Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.tvc.convivial.twitter.TwitterClient.{AccessToken, RequestToken, User, Verifier}
import org.http4s.client.Client
import org.http4s.client.oauth1.{Consumer, Token}
import org.http4s.headers.{Authorization, Location}
import org.http4s.syntax.literals._
import org.http4s.syntax.string._
import org.http4s._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TwitterClientTest extends AnyWordSpec with Matchers {

  def logCalls[F[_] : Sync, A](client: Client[F])(run: Client[F] => F[A]): F[List[Request[F]]] =
    for {
      ref <- Ref.of[F, List[Request[F]]](List.empty)
      _ <- run(Client(req => Resource.liftF(ref.update(_ :+ req)) >> client.run(req)))
      requests <- ref.get
    } yield requests

  def okClient[F[_] : Sync, A](a: A)(implicit e: EntityEncoder[F, A]): Client[F] =
    Client(_ => Resource.pure(Response[F]().withEntity(a)))

  val config: TwitterClient.Config =
    TwitterClient.Config(Consumer("foo", "bar"), uri"http://localhost")

  "TwitterClient.requestToken" should {

    val okResponse: UrlForm = UrlForm(
      "oauth_token" -> "token_parsed",
      "oauth_token_secret" -> "token_secret_parsed",
      "oauth_callback_confirmed" -> "true"
    )

    "Parse the resulting forms okay" in {
      val res = TwitterClient[IO](config, okClient(okResponse)).requestToken.unsafeRunSync()
      res shouldBe RequestToken(Token("token_parsed", "token_secret_parsed"), oauthCallbackConfirmed = true)
    }

    "Make correct looking HTTP calls" in {
      val res: Request[IO] = logCalls[IO, RequestToken](okClient(okResponse)) { client =>
        TwitterClient[IO](config, client).requestToken
      }.unsafeRunSync.head

      res.method shouldBe Method.GET
      res.uri shouldBe uri"https://api.twitter.com/oauth/request_token"
      res.headers.get(Authorization).map(_.credentials.authScheme) shouldBe Some("OAuth".ci)
      res.headers.toString().contains(config.consumer.secret) shouldBe false
    }
  }

  "TwitterClient.redirect" should {

    "Make correct looking redirects" in {
      val expectedLocation = Location(uri"https://api.twitter.com/oauth/authenticate?oauth_token=req")
      val rt: RequestToken = RequestToken(Token("req", "req_s"), oauthCallbackConfirmed = true)
      val redirect = TwitterClient[IO](config, okClient("")).redirect(rt).unsafeRunSync()
      redirect.headers.get(Location) shouldBe Some(expectedLocation)
      redirect.status shouldBe Status.TemporaryRedirect
    }

    "Refuse to make a redirect if oauthCallbackConfirmed is false" in {
      val rt: RequestToken = RequestToken(Token("req", "req_s"), oauthCallbackConfirmed = false)
      val redirect = TwitterClient[IO](config, okClient("")).redirect(rt).attempt.unsafeRunSync()
      redirect should matchPattern { case Left(_) => }
    }
  }

  "TwitterClient.accessToken" should {

    val rt: RequestToken = RequestToken(Token("req", "req_s"), oauthCallbackConfirmed = true)
    val verifier: Verifier = Verifier("foo")

    val token: UrlForm = UrlForm(
      "oauth_token" -> "token_parsed",
      "oauth_token_secret" -> "token_secret_parsed",
    )

    "Parse the resulting forms okay" in {
      val res = TwitterClient[IO](config, okClient(token)).accessToken(verifier, rt).unsafeRunSync()
      res shouldBe AccessToken(Token("token_parsed", "token_secret_parsed"))
    }

    "Make correct looking HTTP calls" in {
      val res: Request[IO] = logCalls[IO, AccessToken](okClient(token)) { client =>
        TwitterClient[IO](config, client).accessToken(verifier, rt)
      }.unsafeRunSync.head

      res.method shouldBe Method.GET
      res.uri shouldBe uri"https://api.twitter.com/oauth/access_token"
      res.headers.get(Authorization).map(_.credentials.authScheme) shouldBe Some("OAuth".ci)
      res.headers.toString().contains(config.consumer.secret) shouldBe false
      res.headers.toString().contains(rt.token.secret) shouldBe false
    }
  }

  "TwitterClient.verifyCredentials" should {

    val at: AccessToken = AccessToken(Token("at", "at_s"))
    val userJson = s"""{"name": "foo", "id_str": "baz"}"""
    val user: User = User("foo", TwitterId("baz"))

    "Parse the resulting user okay" in {
      TwitterClient[IO](config, okClient(userJson)).verifyCredentials(at).unsafeRunSync() shouldBe user
    }

    "Make correct looking HTTP calls" in {
      val res: Request[IO] = logCalls[IO, User](okClient(userJson)) { client =>
        TwitterClient[IO](config, client).verifyCredentials(at)
      }.unsafeRunSync.head

      res.method shouldBe Method.GET
      res.uri shouldBe uri"https://api.twitter.com/1.1/account/verify_credentials.json"
      res.headers.get(Authorization).map(_.credentials.authScheme) shouldBe Some("OAuth".ci)
      res.headers.toString().contains(config.consumer.secret) shouldBe false
      res.headers.toString().contains(at.value.secret) shouldBe false
    }
  }
}
