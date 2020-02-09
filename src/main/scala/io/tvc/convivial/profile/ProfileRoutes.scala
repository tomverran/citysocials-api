package io.tvc.convivial.profile

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.tvc.convivial.users.User
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

class ProfileRoutes[F[_]: Sync](ps: ProfileStorage[F]) extends Http4sDsl[F] {

  val routes: AuthedRoutes[User.Id, F] =
    AuthedRoutes.of {
      case GET -> Root / "api" / "profile" as user =>
        ps.get(user).flatMap(Ok(_))
      case r @ PUT -> Root / "api" / "profile" as user =>
        for {
          profile <- r.req.as[Profile.Complete]
          _ <- ps.put(user, profile)
          res <- Ok(profile: Profile)
        } yield res
    }
}
