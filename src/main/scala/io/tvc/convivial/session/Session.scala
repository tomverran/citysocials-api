package io.tvc.convivial.session

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.instances.option._
import cats.syntax.traverse._
import io.tvc.convivial.session.IdCreator.SessionId
import org.http4s.headers.Cookie
import org.http4s.{AuthedRequest, AuthedRoutes, HttpRoutes, ResponseCookie}
import cats.syntax.functor._
/**
  * Scarily DIY cookie based session ID middleware -
  * retrieves the session ID for you if it exists & if not creates one + writes it back to the client
  */
object Session {

  private val sessId: String = "id"
  type SessionRoutes[F[_]] = AuthedRoutes[SessionId, F]

  def apply[F[_]](ids: IdCreator[F], routes: SessionRoutes[F])(implicit F: Sync[F]): HttpRoutes[F] =
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
}
