package io.tvc.convivial.users

import cats.effect.Sync
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

class UserRoutes[F[_]: Sync] extends Http4sDsl[F] {

  val routes: AuthedRoutes[User.Id, F] =
    AuthedRoutes.of {
      case GET -> Root / "api" / "user" as _ =>
        Ok("")
    }
}
