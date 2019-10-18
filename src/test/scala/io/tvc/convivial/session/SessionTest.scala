package io.tvc.convivial.session

import java.util.UUID

import cats.data.Kleisli
import cats.data.NonEmptyList.fromListUnsafe
import cats.effect.IO
import io.tvc.convivial.session.IdCreator.SessionId
import org.http4s.headers.Cookie
import org.http4s.{Request, RequestCookie, Response, ResponseCookie}
import org.scalatest.{Matchers, WordSpec}

class SessionTest extends WordSpec with Matchers {

  val stubIds: IdCreator[IO] = new IdCreator[IO] {
    def create: IO[IdCreator.SessionId] = IO.pure(SessionId(UUID.randomUUID.toString))
    def verify(s: String): IO[IdCreator.SessionId] = IO.pure(SessionId(s))
  }

  val failingIds: IdCreator[IO] = new IdCreator[IO] {
    def create: IO[SessionId] = stubIds.create
    def verify(s: String): IO[SessionId] = IO.raiseError(new Exception("verification failed"))
  }

  val request: Request[IO] = Request[IO]()
  val response: Response[IO] = Response[IO]()

  "Session middleware" should {

    "Create a new session cookie where one is not passed" in {
      val resp = Session[IO](stubIds, Kleisli.pure(response)).run(request).value.unsafeRunSync().get
      val sessionId: ResponseCookie = resp.cookies.head
      sessionId.path shouldBe Some("/")
      sessionId.httpOnly shouldBe true
      sessionId.name shouldBe "id"
    }

    "Reuse a session ID when it is passed along" in {
      (
        for {
          first <- Session[IO](stubIds, Kleisli.pure(response)).run(request)
          cookies = Cookie(fromListUnsafe(first.cookies.map(c => RequestCookie(c.name, c.content))))
          second <- Session[IO](stubIds, Kleisli.pure(response)).run(request.putHeaders(cookies))
        } yield second.cookies.find(_.name == "id") shouldBe first.cookies.find(_.name == "id")
      ).value.unsafeRunSync().get
    }

    "Ignore a session cookie if session ID verification fails" in {
      (
        for {
          first <- Session[IO](stubIds, Kleisli.pure(response)).run(Request())
          cookies = Cookie(fromListUnsafe(first.cookies.map(c => RequestCookie(c.name, c.content))))
          second <- Session[IO](failingIds, Kleisli.pure(response)).run(Request[IO]().putHeaders(cookies))
        } yield {
          val badCookie = cookies.values.find(_.name == "id").map(_.content)
          second.cookies.find(_.name == "id").map(_.content) should not be badCookie
        }
      ).value.unsafeRunSync().get
    }
  }
}
