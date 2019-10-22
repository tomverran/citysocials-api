package io.tvc.convivial.users

import cats.effect.Sync
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec._

class UserRoutes[F[_]: Sync] extends Http4sDsl[F] {

  val routes: AuthedRoutes[User, F] =
    AuthedRoutes.of {
      case GET -> Root / "api" / "user" as user => Ok(user.asJson)
    }
}
