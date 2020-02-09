package io.tvc.convivial.profile

import cats.effect.IO
import io.tvc.convivial.users.authedRequest
import org.http4s.Method.PUT
import org.http4s.Request
import org.http4s.circe.CirceEntityCodec._
import org.http4s.syntax.literals._
import org.scalacheck.Prop
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.claimant.Claim

class ProfileRoutesTest extends AnyWordSpec with Matchers with Checkers {

  val getProfile: Request[IO] = Request[IO](uri = uri"http://localhost/api/profile")
  def putProfile(p: Profile.Complete): Request[IO] = getProfile.withEntity(p).withMethod(PUT)

  "Return Incomplete if you've not got a profile" in {
    check(
      Prop.forAll(authedRequest(getProfile)) { req =>
        (
          for {
            storage <- TestProfileStorage.apply[IO]
            routes = new ProfileRoutes[IO](storage).routes
            result <- routes.run(req).semiflatMap(_.as[Profile]).value
          } yield Claim(result.contains(Profile.Incomplete))
        ).unsafeRunSync()
      }
    )
  }

  "Return a pre-existing profile if you've got one" in {
    check(
      Prop.forAll(complete, authedRequest(getProfile)) { case (prof, req) =>
        (
          for {
            storage <- TestProfileStorage.apply[IO]
            _       <- storage.put(req.context, prof)
            routes = new ProfileRoutes[IO](storage).routes
            result <- routes.run(req).semiflatMap(_.as[Profile]).value
          } yield Claim(result.contains(prof))
        ).unsafeRunSync()
      }
    )
  }

  "Save a new profile when called with PUT" in {
    check(
      Prop.forAll(
        for {
          prof <- complete
          req <- authedRequest(putProfile(prof))
        } yield (prof, req)
      ) { case (prof, req) =>
        (
          for {
            storage <- TestProfileStorage.apply[IO]
            routes = new ProfileRoutes[IO](storage).routes
            result <- routes.run(req).semiflatMap(_.as[Profile]).value
            saved <- storage.get(req.context)
          } yield Claim(result.contains(saved) && saved == prof)
        ).unsafeRunSync()
      }
    )
  }
}
