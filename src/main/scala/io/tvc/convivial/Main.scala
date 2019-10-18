package io.tvc.convivial

import cats.effect.ExitCode.Error
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.functor._
import io.tvc.convivial.session.{IdCreator, Session}
import io.tvc.convivial.twitter.{TokenStorage, TwitterClient, TwitterSSO}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  val http: Resource[IO, Client[IO]] =
    BlazeClientBuilder[IO](global).withDefaultSslContext.resource

  val config: Resource[IO, Config] =
    Resource.liftF(Config.load[IO])

  def run(args: List[String]): IO[ExitCode] =
    (
      for {
        http   <- http
        config <- config
        storage <- TokenStorage.toy[IO]
        ids <- IdCreator.apply[IO](config.session)
      } yield Session(ids, new TwitterSSO[IO](TwitterClient(config.twitter, http), storage).routes)
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
