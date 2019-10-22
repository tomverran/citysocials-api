package io.tvc.convivial.storage

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import io.circe.{Decoder, Encoder, Json}

trait TestRedis[F[_]] extends Redis[F] {
  def written: F[Map[String, Json]]
}

object TestRedis {

  def apply[F[_]: Sync]: F[TestRedis[F]] =
    Ref.of[F, Map[String, Json]](Map.empty).map { ref =>
      new TestRedis[F] {
        def written: F[Map[String, Json]] =
          ref.get
        def put[A: Encoder](key: String, value: A): F[Unit] =
          ref.update(_.updated(key, Encoder[A].apply(value)))
        def get[A: Decoder](key: String): F[Option[A]] =
          ref.get.map(_.get(key).flatMap(Decoder[A].decodeJson(_).toOption))
      }
    }
}
