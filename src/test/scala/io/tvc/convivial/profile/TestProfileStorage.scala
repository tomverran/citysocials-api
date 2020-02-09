package io.tvc.convivial.profile

import cats.effect.Sync
import cats.effect.concurrent.Ref
import io.tvc.convivial.users.User
import cats.syntax.functor._
import io.tvc.convivial.profile.Profile.Incomplete

trait TestProfileStorage[F[_]] extends ProfileStorage[F] {
  // I don't need any extra methods atm but I daresay I will
}

object TestProfileStorage {

  /**
    * Toy implementation of profile storage
    * that just uses a ref of a map for now
    */
  def apply[F[_]](implicit F: Sync[F]): F[TestProfileStorage[F]] =
    Ref.of[F, Map[User.Id, Profile.Complete]](Map.empty).map { ref =>
      new TestProfileStorage[F] {
        def put(user: User.Id, p: Profile.Complete): F[Unit] = ref.update(_.updated(user, p))
        def get(user: User.Id): F[Profile] = ref.get.map(_.getOrElse(user, Incomplete))
      }
    }
}
