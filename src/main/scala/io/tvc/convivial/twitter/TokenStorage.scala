package io.tvc.convivial.twitter

import cats.effect.concurrent.Ref
import cats.effect.{Resource, Sync}
import cats.syntax.functor._
import io.tvc.convivial.http.Session.SessionId
import io.tvc.convivial.twitter.TwitterClient.RequestToken

trait TokenStorage[F[_]] {
  def store(sessionId: SessionId, requestToken: RequestToken): F[Unit]
  def retrieve(sessionId: SessionId): F[Option[RequestToken]]
}

object TokenStorage {

  /**
    * Toy implementation of token storage
    * that just uses a ref of a map for now
    */
  def toy[F[_]: Sync]: Resource[F, TokenStorage[F]] =
    Resource.liftF(
      Ref.of[F, Map[SessionId, RequestToken]](Map.empty).map { ref =>
        new TokenStorage[F] {
          def store(sessionId: SessionId, requestToken: RequestToken): F[Unit] =
            ref.update(_.updated(sessionId, requestToken))
          def retrieve(sessionId: SessionId): F[Option[RequestToken]] =
            ref.get.map(_.get(sessionId))
        }
      }
    )
}
