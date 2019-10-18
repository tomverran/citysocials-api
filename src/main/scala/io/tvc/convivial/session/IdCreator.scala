package io.tvc.convivial.session

import java.math.BigInteger
import java.security.SecureRandom

import cats.effect.{Resource, Sync}
import com.google.common.hash.{HashCode, HashFunction, Hashing}
import com.google.common.io.BaseEncoding
import com.google.common.io.BaseEncoding.base16
import io.tvc.convivial.session.IdCreator.SessionId

/**
  * Produces session IDs consisting of a random string
  * and an HMAC of the underlying bytes to verify integrity
  */
trait IdCreator[F[_]] {
  def create: F[SessionId]
  def verify(s: String): F[SessionId]
}

object IdCreator {

  case class Config(secretKey: String) extends AnyVal
  case class SessionId(value: String) extends AnyVal

  def apply[F[_]](config: Config)(implicit F: Sync[F]): Resource[F, IdCreator[F]] = {

    Resource.liftF {
      Sync[F].delay {

        val random: SecureRandom = SecureRandom.getInstanceStrong
        val mac: HashFunction = Hashing.hmacSha256(config.secretKey.getBytes)
        val hex: BaseEncoding = base16()

        def fail[A]: F[A] =
          F.raiseError(new Exception("Failed to verify session ID"))

        new IdCreator[F] {

          def create: F[SessionId] =
            F.delay {
              val id: Array[Byte] = new BigInteger(256, random).toByteArray
              SessionId(s"${hex.encode(id)}.${mac.hashBytes(id).toString}")
            }

          def verify(s: String): F[SessionId] = {
            s.split('.').toList match {
              case id :: auth :: Nil if mac.hashBytes(hex.decode(id)) == HashCode.fromString(auth) =>
               F.pure(SessionId(s))
              case _ =>
                fail
            }
          }
        }
      }
    }
  }
}
