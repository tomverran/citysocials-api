package io.tvc.convivial.session

import java.util.UUID

import cats.data.NonEmptyList.fromListUnsafe
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.instances.option._
import cats.syntax.functor._
import cats.syntax.traverse._
import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.session.SessionMiddleware._
import io.tvc.convivial.users.User
import org.http4s.circe.CirceEntityCodec._
import org.http4s.headers.{Cookie, `Cache-Control`}
import org.http4s._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SessionMiddlewareTest extends AnyWordSpec with Matchers {

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

  "Session.id middleware" should {

    "Create a new session cookie where one is not passed" in {
      val resp = id[IO](stubIds, Kleisli.pure(response)).run(request).value.unsafeRunSync().get
      val sessionId: ResponseCookie = resp.cookies.head
      sessionId.path shouldBe Some("/")
      sessionId.httpOnly shouldBe true
      sessionId.name shouldBe "id"
    }

    "Reuse a session ID when it is passed along" in {
      (
        for {
          first <- id[IO](stubIds, Kleisli.pure(response)).run(request)
          cookies = Cookie(fromListUnsafe(first.cookies.map(c => RequestCookie(c.name, c.content))))
          second <- id[IO](stubIds, Kleisli.pure(response)).run(request.putHeaders(cookies))
        } yield second.cookies.find(_.name == "id") shouldBe first.cookies.find(_.name == "id")
      ).value.unsafeRunSync().get
    }

    "Disable caching on all requests going through it" in {
      val resp = id[IO](stubIds, Kleisli.pure(response)).run(request).value.unsafeRunSync().get
      resp.headers.get(`Cache-Control`).map(_.values.head) shouldBe Some(CacheDirective.`no-cache`())
    }

    "Ignore a session cookie if session ID verification fails" in {
      (
        for {
          first <- id[IO](stubIds, Kleisli.pure(response)).run(Request())
          cookies = Cookie(fromListUnsafe(first.cookies.map(c => RequestCookie(c.name, c.content))))
          second <- id[IO](failingIds, Kleisli.pure(response)).run(Request[IO]().putHeaders(cookies))
        } yield {
          val badCookie = cookies.values.find(_.name == "id").map(_.content)
          second.cookies.find(_.name == "id").map(_.content) should not be badCookie
        }
      ).value.unsafeRunSync().get
    }
  }

  "Session.user middleware" should {

    "Go to SessionStorage to swap a session ID for a user ID before passing it to the route" in {
      Ref.of[IO, List[SessionId]](List.empty).flatMap { ref =>

        val authUser = User.Id(213)
        val routes: AuthedRoutes[User.Id, IO] = Kleisli(r => OptionT.some(response.withEntity(r.context)))

        val store = new SessionStorage[IO] {
          def put(sId: SessionId, user: User.Id): IO[Unit] = IO.raiseError(new Exception)
          def get(sId: SessionId): IO[Option[User.Id]] = ref.update(_ :+ sId).as(Option(authUser))
        }

        for {
          res <- user[IO](stubIds, store, routes).run(request).value
          resolved <- res.traverse(_.as[User.Id])
          sessionId <- ref.get.map(_.head)
        } yield {
          resolved shouldBe Some(authUser)
          res.flatMap(_.cookies.headOption.map(_.content)) shouldBe Some(sessionId.value)
        }
      }.unsafeRunSync()
    }
  }
}
