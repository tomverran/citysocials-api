package io.tvc.convivial.twitter

import cats.effect.IO
import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.twitter.TwitterClient.{AccessToken, RequestToken, User, Verifier}
import org.http4s.client.oauth1.Token
import org.http4s.syntax.literals._
import org.http4s.{AuthedRequest, Request, Response, Status}
import org.http4s.circe.CirceEntityCodec._
import org.scalatest.{Matchers, WordSpec}
import cats.syntax.flatMap._

class TwitterSSOTest extends WordSpec with Matchers {

  "Twitter SSO routes" should {

    val sessId = SessionId("abcd")
    val user: User = User("person", "screen", Some("foo@bar.com"))
    val request = RequestToken(Token("rt", "rt_sec"), oauthCallbackConfirmed = true)
    val redir = Response[IO](status = Status.ExpectationFailed)
    val access = AccessToken(Token("at", "at_sec"))

    val client: TwitterClient[IO] =
      new TwitterClient[IO] {
        def requestToken: IO[RequestToken] = IO.pure(request)
        def redirect(requestToken: RequestToken): IO[Response[IO]] = IO.pure(redir)
        def accessToken(verifier: Verifier, requestToken: RequestToken): IO[AccessToken] = IO.pure(access)
        def verifyCredentials(accessToken: AccessToken): IO[User] = IO.pure(user)
      }

    "Get a refresh token from twitter and send you there when going to /twitter/login" in {
      val (redirect, session) = TokenStorage.toy[IO].use { storage =>

        val request = AuthedRequest(sessId, Request[IO](uri = uri"http://localhost/twitter/login"))
        val routes = new TwitterSSO[IO](client, storage).routes

        for {
          redirect <- routes.run(request).value
          session <- storage.retrieve(sessId)
        } yield redirect -> session
      }.unsafeRunSync()

      redirect shouldBe Some(redir)
      session shouldBe Some(request)
    }

    "Kick you out if when returning from twitter your OAuth token does not match the one stored" in {
      TokenStorage.toy[IO].use { storage =>
        val verifyUri = uri"http://foo/twitter/verify?oauth_token=bar&oauth_verifier=baz"
        val verifyRequest = AuthedRequest(sessId, Request[IO](uri = verifyUri))
        val routes = new TwitterSSO[IO](client, storage).routes
        storage.store(sessId, request) >> routes.run(verifyRequest).value
      }.attempt.unsafeRunSync() should matchPattern { case Left(_) => }
    }

    "Return your user details if the verify request succeeds" in {
      TokenStorage.toy[IO].use { storage =>
        val verifyUri = uri"http://foo/twitter/verify?oauth_token=rt&oauth_verifier=baz"
        val verifyRequest = AuthedRequest(sessId, Request[IO](uri = verifyUri))
        val routes = new TwitterSSO[IO](client, storage).routes
        storage.store(sessId, request) >> routes.run(verifyRequest).semiflatMap(_.as[User]).value
      }.unsafeRunSync() shouldBe Some(user)
    }
  }
}
