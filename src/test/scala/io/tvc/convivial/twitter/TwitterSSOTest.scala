package io.tvc.convivial.twitter

import cats.effect.IO
import cats.syntax.flatMap._
import io.tvc.convivial
import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.session.{SessionStorage, TestSessionStorage}
import io.tvc.convivial.twitter.TwitterClient.{AccessToken, RequestToken, User, Verifier}
import io.tvc.convivial.users
import io.tvc.convivial.users.{TestUserStorage, UserStorage}
import org.http4s.client.oauth1.Token
import org.http4s.syntax.literals._
import org.http4s.{AuthedRequest, Request, Response, Status}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TwitterSSOTest extends AnyWordSpec with Matchers {

  "Twitter SSO routes" should {

    val sessId = SessionId("abcd")
    val user: User = User("person", TwitterId("screen"))
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

    val failSessionStore: SessionStorage[IO] =
      new SessionStorage[IO] {
        def put(sessionId: SessionId, user: users.User.Id): IO[Unit] = IO.raiseError(new Exception)
        def get(sessionId: SessionId): IO[Option[users.User.Id]] = IO.raiseError(new Exception)
      }

    val userStore: UserStorage[IO] =
      _ => IO.pure(users.User.Id(1234))

    "Get a refresh token from twitter and send you there when going to /twitter/login" in {
      val (redirect, session) = TestTokenStorage.apply[IO].flatMap { storage =>

        val request = AuthedRequest(sessId, Request[IO](uri = uri"http://localhost/twitter/login"))
        val routes = new TwitterSSO[IO](client, storage, userStore, failSessionStore).routes

        for {
          redirect <- routes.run(request).value
          session <- storage.retrieve(sessId)
        } yield redirect -> session
      }.unsafeRunSync()

      redirect shouldBe Some(redir)
      session shouldBe Some(request)
    }

    "Kick you out if when returning from twitter your OAuth token does not match the one stored" in {
      TestTokenStorage.apply[IO].flatMap { storage =>
        val verifyUri = uri"http://foo/twitter/verify?oauth_token=bar&oauth_verifier=baz"
        val verifyRequest = AuthedRequest(sessId, Request[IO](uri = verifyUri))
        val routes = new TwitterSSO[IO](client, storage, userStore, failSessionStore).routes
        storage.store(sessId, request) >> routes.run(verifyRequest).value
      }.attempt.unsafeRunSync() should matchPattern { case Left(_) => }
    }

    "Return your user details if the verify request succeeds" in {
      val verifyUri = uri"http://foo/twitter/verify?oauth_token=rt&oauth_verifier=baz"
      val verifyRequest = AuthedRequest(sessId, Request[IO](uri = verifyUri))
      (
        for {
          users <- TestUserStorage.apply[IO]
          tokens <- TestTokenStorage.apply[IO]
          sessions <- TestSessionStorage.apply[IO]
          routes = new TwitterSSO(client, tokens, users, sessions).routes
          _ <- tokens.store(sessId, request) >> routes.run(verifyRequest).value
          writtenSession <- sessions.get(sessId)
          writtenIds <- users.writtenIds
          writtenUsers <- users.written
        } yield {
          writtenUsers shouldBe List(convivial.users.User(user.name, user.idStr))
          writtenSession shouldBe writtenIds.headOption
        }
      ).unsafeRunSync()
    }
  }
}
