package io.tvc.convivial.users
import cats.data.OptionT
import cats.effect.Sync
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.{Get, Put}
import io.tvc.convivial.twitter
import io.tvc.convivial.twitter.TwitterId

trait UserStorage[F[_]] {
  def upsert(user: User): F[User.Id]
}

object UserStorage {

  implicit val putUid: Put[User.Id] = Put[Int].contramap(_.value)
  implicit val getUid: Get[User.Id] = Get[Int].map(User.Id.apply)

  implicit val putTwitterId: Put[TwitterId] = Put[String].contramap(_.value)
  implicit val getTwitterId: Get[TwitterId] = Get[String].map(twitter.TwitterId.apply)

  private def find(id: TwitterId): ConnectionIO[Option[User.Id]] =
    sql"SELECT id FROM users WHERE twitter_id = $id".query[User.Id].option

  private def insert(u: User): ConnectionIO[User.Id] =
    sql"""
    INSERT INTO users (name, twitter_id, created_at)
    VALUES (${u.name},${u.twitterId}, NOW())
    """.update.withUniqueGeneratedKeys("id")

  def postgres[F[_]: Sync](transactor: Transactor[F]): UserStorage[F] =
    u => OptionT(find(u.twitterId)).getOrElseF(insert(u)).transact(transactor)
}
