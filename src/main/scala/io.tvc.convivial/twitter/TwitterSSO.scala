package io.tvc.convivial.twitter

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.tvc.convivial.http.Session.SessionId
import io.tvc.convivial.twitter.TwitterClient.Verifier
import org.http4s.{AuthedRoutes, Response}
import org.http4s.QueryParamDecoder.{stringQueryParamDecoder => str}
import cats.syntax.traverse._
import cats.instances.option._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

class TwitterSSO[F[_]: Sync](twitter: TwitterClient[F], store: TokenStorage[F]) extends Http4sDsl[F] {

  object Verify extends QueryParamDecoderMatcher[Verifier]("oauth_verifier")(str.map(Verifier.apply))
  object Token extends QueryParamDecoderMatcher[String]("oauth_token")(str)
  private val badToken = new Exception("oauth_token mismatch")

  val routes: AuthedRoutes[SessionId, F] =
    AuthedRoutes.of[SessionId, F] {

      case GET -> Root / "twitter" / "login" as id =>
        for {
          token <- twitter.requestToken
          _     <- store.store(id, token)
          redirect <- twitter.redirect(token)
        } yield redirect

      case GET -> Root / "twitter" / "verify" :? Token(t) +& Verify(v) as id =>
        for {
          requestToken <- store.retrieve(id)
          _ <- Sync[F].fromEither(requestToken.filter(_.token.value == t).toRight(badToken))
          accessToken <- requestToken.traverse(twitter.accessToken(v, _))
          user <- accessToken.traverse(twitter.verifyCredentials)
        } yield Response[F]().withEntity(user)

    }
}

