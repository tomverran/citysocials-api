package io.tvc.convivial.twitter

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.session.SessionStorage
import io.tvc.convivial.twitter.TwitterClient.{RequestToken, Verifier}
import io.tvc.convivial.users.{User, UserStorage}
import org.http4s.QueryParamDecoder.{stringQueryParamDecoder => str}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.syntax.literals._
import org.http4s.{AuthedRoutes, Headers, Response}

class TwitterSSO[F[_]](
  twitter: TwitterClient[F],
  tokens: TokenStorage[F],
  users: UserStorage[F],
  sessions: SessionStorage[F]
)(implicit F: Sync[F]) extends Http4sDsl[F] {

  object Verify extends QueryParamDecoderMatcher[Verifier]("oauth_verifier")(str.map(Verifier.apply))
  object Token extends QueryParamDecoderMatcher[String]("oauth_token")(str)

  private def checkToken(original: RequestToken, now: String): F[Unit] =
    if (original.token.value != now) F.raiseError(new Exception("oauth_token mismatch")) else F.unit

  val routes: AuthedRoutes[SessionId, F] =
    AuthedRoutes.of[SessionId, F] {

      case GET -> Root / "twitter" / "login" as id =>
        for {
          token <- twitter.requestToken
          _     <- tokens.store(id, token)
          redirect <- twitter.redirect(token)
        } yield redirect

      case GET -> Root / "twitter" / "verify" :? Token(t) +& Verify(v) as id =>
        OptionT(tokens.retrieve(id)).semiflatMap { requestToken =>
          for {
            _ <- checkToken(requestToken, t)
            accessToken <- twitter.accessToken(v, requestToken)
            credentials <- twitter.verifyCredentials(accessToken)
            user = User(credentials.name, credentials.idStr)
            userId <- users.upsert(user)
            _ <- sessions.put(id, userId)
          } yield Response[F](
            status = TemporaryRedirect,
            headers = Headers.of(Location(uri"/#profile"))
          )
        }.getOrElseF(BadRequest(""))
    }
}

