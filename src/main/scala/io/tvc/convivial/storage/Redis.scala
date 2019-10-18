package io.tvc.convivial.storage

import cats.data.OptionT
import cats.effect.{ConcurrentEffect, ContextShift, Resource, Sync}
import dev.profunktor.redis4cats.connection.{RedisClient, RedisURI}
import dev.profunktor.redis4cats.domain.RedisCodec.Utf8
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.interpreter.{Redis => RedisInstance}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}

import scala.concurrent.duration.FiniteDuration

/**
  * A very basic API for other things to build upon
  * the plan is just to use this for session storage of JSON
  */
trait Redis[F[_]] {
  def put[A: Encoder](key: String, value: A): F[Unit]
  def get[A: Decoder](key: String): F[Option[A]]
}

object Redis {

  case class Config(
    url: String,
    ttl: FiniteDuration
  )

  def apply[F[_]: ConcurrentEffect: Log: ContextShift](config: Config): Resource[F, Redis[F]] =
    for {
      uri <- Resource.liftF(RedisURI.make(config.url))
      client <- RedisClient.apply(uri)
      redis <- RedisInstance[F, String, String](client, Utf8, uri)
    } yield new Redis[F] {
      def put[A: Encoder](key: String, value: A): F[Unit] =
        redis.setEx(key, Encoder[A].apply(value).noSpaces, config.ttl)
      def get[A: Decoder](key: String): F[Option[A]] =
        OptionT(redis.get(key)).semiflatMap(s => Sync[F].fromEither(decode[A](s))).value
    }
}
