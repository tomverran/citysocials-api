package io.tvc.convivial.profile

import cats.effect.{Clock, Sync}
import cats.instances.option._
import cats.syntax.functor._
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.util.transactor.Transactor
import io.chrisdavenport.cats.effect.time.JavaTime
import io.tvc.convivial.profile.Profile.Incomplete
import io.tvc.convivial.users.User
import cats.syntax.flatMap._

trait ProfileStorage[F[_]] {
  def put(user: User.Id, profile: Profile.Complete): F[Unit]
  def get(user: User.Id): F[Profile]
}

object ProfileStorage {

  def postgres[F[_]: Sync: Clock](transactor: Transactor[F]): ProfileStorage[F] =
    new ProfileStorage[F] {

      def put(user: User.Id, profile: Profile.Complete): F[Unit] =
        JavaTime[F].getInstant.flatMap { time =>
          sql"""
          INSERT INTO user_profiles (user_id, location, interests, created_at)
          VALUES (${user.value}, ${profile.location}, ${profile.interests}, $time)
          ON CONFLICT (user_id) DO UPDATE SET
            location = excluded.location,
            interests = excluded.interests
          """.update.run.transact(transactor).as(())
        }

      def get(user: User.Id): F[Profile] =
        sql"""
        SELECT location, interests FROM user_profiles WHERE user_id = ${user.value}
        """.query[Profile.Complete].option.transact(transactor).map(_.widen.getOrElse(Incomplete))
    }
}
