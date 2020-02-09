package io.tvc.convivial.session

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.users.User

trait TestSessionStorage[F[_]] extends SessionStorage[F] {
  def written: F[List[SessionId]]
}

object TestSessionStorage {

  /**
    * Toy implementation of session storage
    * that just uses a ref of a map for now
    */
  def apply[F[_]: Sync]: F[TestSessionStorage[F]] =
    Ref.of[F, Map[SessionId, User.Id]](Map.empty).map { ref =>
      new TestSessionStorage[F] {
        def put(sessionId: SessionId, user: User.Id): F[Unit] =
          ref.update(_.updated(sessionId, user))
        def get(sessionId: SessionId): F[Option[User.Id]] =
          ref.get.map(_.get(sessionId))
        def written: F[List[SessionId]] =
          ref.get.map(_.keys.toList)
      }
    }
}
