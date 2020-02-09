package io.tvc.convivial

import io.tvc.convivial.twitter.twitterId
import org.http4s.{AuthedRequest, Request}
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Gen.alphaNumStr

package object users {

  val userId: Gen[User.Id] =
    Arbitrary.arbInt.arbitrary.map(_.abs).map(User.Id.apply)

  val user: Gen[User] =
    for {
      name <- alphaNumStr
      twitter <- twitterId
    } yield User(name, twitter)

  def authedRequest[F[_]](a: Request[F]): Gen[AuthedRequest[F, User.Id]] =
    userId.map(AuthedRequest(_, a))
}
