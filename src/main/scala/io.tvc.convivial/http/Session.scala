package io.tvc.convivial.http
import java.math.BigInteger
import java.security.SecureRandom

import cats.data.OptionT.fromOption
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import org.http4s.headers.Cookie
import org.http4s.{AuthedRequest, AuthedRoutes, HttpRoutes, ResponseCookie}

/**
  * Scarily DIY cookie based session ID middleware -
  * retrieves the session ID for you if it exists & if not creates one + writes it back to the client
  */
object Session {

  case class SessionId(value: String) extends AnyVal
  private val idCookie: String = "id"

  object SessionId {

    private val random: SecureRandom =
      new SecureRandom

    def create[F[_]: Sync]: F[SessionId] =
      Sync[F].delay(SessionId(new BigInteger(256, random).toString(16)))
  }

  def middleware[F[_]: Sync](routes: AuthedRoutes[SessionId, F]): HttpRoutes[F] =
    Kleisli { req =>

      val cookies = req.headers.get(Cookie).toList.flatMap(_.values.toList)
      val existing = cookies.collectFirst { case c if c.name == idCookie => SessionId(c.content) }
      val session = OptionT.liftF(fromOption(existing).getOrElseF(SessionId.create[F]))

      for {
        id <- session
        result <- routes(AuthedRequest(id, req))
      } yield result.addCookie(ResponseCookie(idCookie, id.value, httpOnly = true))
    }
}
