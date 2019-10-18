package io.tvc.convivial.twitter

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.twitter.TwitterClient.RequestToken

object TestTokenStorage {

  /**
    * Toy implementation of token storage
    * that just uses a ref of a map for now
    */
  def apply[F[_]: Sync]: F[TokenStorage[F]] =
    Ref.of[F, Map[SessionId, RequestToken]](Map.empty).map { ref =>
      new TokenStorage[F] {
        def store(sessionId: SessionId, requestToken: RequestToken): F[Unit] =
          ref.update(_.updated(sessionId, requestToken))
        def retrieve(sessionId: SessionId): F[Option[RequestToken]] =
          ref.get.map(_.get(sessionId))
      }
    }
}
