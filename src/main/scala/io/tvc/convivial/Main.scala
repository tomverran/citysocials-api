package io.tvc.convivial

import cats.effect.ExitCode.Error
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.functor._
import dev.profunktor.redis4cats.effect.Log
import io.tvc.convivial.session.{IdCreator, SessionMiddleware, SessionStorage}
import io.tvc.convivial.storage.{Postgres, Redis}
import io.tvc.convivial.twitter.{TokenStorage, TwitterClient, TwitterSSO}
import io.tvc.convivial.users.UserStorage
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  val http: Resource[IO, Client[IO]] =
    BlazeClientBuilder[IO](global).withDefaultSslContext.resource

  val config: Resource[IO, Config] =
    Resource.liftF(Config.load[IO])

  val logger: Logger = LoggerFactory.getLogger("Redis")

  implicit val log: Log[IO] = new Log[IO] {
    def info(msg: => String): IO[Unit] = IO.delay(logger.info(msg))
    def error(msg: => String): IO[Unit] = IO.delay(logger.error(msg))
  }

  def run(args: List[String]): IO[ExitCode] =
    (
      for {
        http   <- http
        config <- config
        redis <- Redis[IO](config.redis)
        ids <- IdCreator[IO](config.session)
        db <- Postgres.transactor[IO](config.postgres)
      } yield {
        val users: UserStorage[IO] = UserStorage(db)
        val twitter = TwitterClient(config.twitter, http)
        val tokens: TokenStorage[IO] = TokenStorage.redis(redis)
        val sessions: SessionStorage[IO] = SessionStorage.redis(redis)
        SessionMiddleware.id(ids, new TwitterSSO[IO](twitter, tokens, users, sessions).routes)
      }
    ).use { routes =>
      BlazeServerBuilder.apply[IO]
        .withHttpApp(routes.orNotFound)
        .bindHttp(8080, "0.0.0.0")
        .withoutBanner
        .serve
        .compile
        .drain
        .as(Error)
    }
}
