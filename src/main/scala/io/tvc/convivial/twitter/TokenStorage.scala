package io.tvc.convivial.twitter

import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.storage.Redis
import io.tvc.convivial.twitter.TwitterClient.RequestToken

trait TokenStorage[F[_]] {
  def store(sessionId: SessionId, requestToken: RequestToken): F[Unit]
  def retrieve(sessionId: SessionId): F[Option[RequestToken]]
}

object TokenStorage {

  def redis[F[_]](redis: Redis[F]): TokenStorage[F] =
    new TokenStorage[F] {
      def store(sessionId: SessionId, requestToken: RequestToken): F[Unit] =
        redis.put(s"${sessionId.value}_rt", requestToken)
      def retrieve(sessionId: SessionId): F[Option[RequestToken]] =
        redis.get(s"${sessionId.value}_rt")
    }
}
