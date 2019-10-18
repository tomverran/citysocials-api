package io.tvc.convivial.session

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.instances.option._
import cats.syntax.traverse._
import io.tvc.convivial.session.IdCreator.SessionId
import org.http4s.headers.Cookie
import org.http4s.{AuthedRequest, AuthedRoutes, HttpRoutes, ResponseCookie}
import cats.syntax.functor._
import io.tvc.convivial.users.User
/**
  * Scarily DIY cookie based session ID middleware -
  * retrieves the session ID for you if it exists & if not creates one + writes it back to the client
  */
object SessionMiddleware {

  private val sessId: String = "id"
  type UserRoutes[F[_]] = AuthedRoutes[User, F]
  type SessionRoutes[F[_]] = AuthedRoutes[SessionId, F]

  /**
    * Middleware to handle creating + setting sessions for
    * otherwise unauthenticated routes that only need a session ID - like login routes
    */
  def id[F[_]](ids: IdCreator[F], routes: SessionRoutes[F])(implicit F: Sync[F]): HttpRoutes[F] =
    Kleisli { req =>

      val session: F[SessionId] = OptionT(
        req.headers.get(Cookie).toList.flatMap(_.values.toList).collectFirst {
          case c if c.name == sessId => F.attempt(ids.verify(c.content)).map(_.toOption)
        }.flatSequence
      ).getOrElseF(ids.create)

      for {
        id <- OptionT.liftF(session)
        result <- routes(AuthedRequest(id, req))
      } yield result.addCookie(ResponseCookie(sessId, id.value, httpOnly = true, path = Some("/")))
    }

  /**
    * Middleware that wraps the above id code
    * and looks up user information from session storage
    * for routes that require actual authentication to have occurred
    */
  def user[F[_]: Sync](ids: IdCreator[F], store: SessionStorage[F], routes: UserRoutes[F]): HttpRoutes[F] =
    id(
      ids = ids,
      routes = Kleisli { r =>
        for {
          user <- OptionT(store.get(r.authInfo))
          result <- routes.run(AuthedRequest(user, r.req))
        } yield result
      }
    )
}
