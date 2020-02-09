package io.tvc.convivial.session

import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.storage.Redis
import io.tvc.convivial.users.User

/**
  * Non persistent storage of user sessions
  * to handle authentication with the API
  */
trait SessionStorage[F[_]] {
  def put(sessionId: SessionId, user: User.Id): F[Unit]
  def get(sessionId: SessionId): F[Option[User.Id]]
}

object SessionStorage {

  def redis[F[_]](r: Redis[F]): SessionStorage[F] =
    new SessionStorage[F] {
      def put(sessionId: SessionId, user: User.Id): F[Unit] = r.put(s"${sessionId.value}_uid", user)
      def get(sessionId: SessionId): F[Option[User.Id]] = r.get(s"${sessionId.value}_uid")
    }
}


