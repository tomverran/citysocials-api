package io.tvc.convivial.users

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.util.Random

trait TestUserStorage[F[_]] extends UserStorage[F] {
  def writtenIds: F[List[User.Id]]
  def written: F[List[User]]

}

object TestUserStorage {

  /**
    * Toy implementation of user storage
    * that just uses a ref of a map for now
    */
  def apply[F[_]](implicit F: Sync[F]): F[TestUserStorage[F]] =
    Ref.of[F, Map[User.Id, User]](Map.empty).map { ref =>
      new TestUserStorage[F] {
        def written: F[List[User]] = ref.get.map(_.values.toList)
        def writtenIds: F[List[User.Id]] = ref.get.map(_.keys.toList)
        def upsert(user: User): F[User.Id] =
          for {
            id <- F.delay(new Random().nextInt)
            _ <- ref.update(_.updated(User.Id(id), user))
          } yield User.Id(id)
      }
    }
}
