package io.tvc.convivial.storage

import cats.effect.{ContextShift, Effect, IO, Resource, Timer}
import cats.instances.list._
import cats.syntax.traverse._
import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{DockerContainerState => DCS, _}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import io.tvc.convivial.storage.Postgres.Config
import org.scalacheck.Prop
import org.scalactic.anyvals.PosInt
import org.scalactic.source
import org.scalatest.Suite
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.Checkers

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * This is all pretty horrible but does the job. It spins up a postgres DB with docker
  * then provides code to clean up any changes made after the test
  */
trait TestPostgres extends DockerTestKit with Checkers {
  self: Suite with AnyWordSpecLike =>

  /**
    * All the tests requiring a database run in IO
    * so we opportunistically provide the instances required here too
    */
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val ti: Timer[IO] = IO.timer(ExecutionContext.global)

  override implicit val dockerFactory: DockerFactory =
    new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = PosInt(500))

  /**
    * Provides a database resource configured to talk to
    * the docker container we're about to run
    */
  def database[F[_] : Effect : ContextShift]: Resource[F, Transactor[F]] =
    Postgres.transactor(Config(s"jdbc:postgresql://${dockerExecutor.host}/", "postgres", "postgres"))

  /**
    * Very brutal ready check that simply
    * polls the database and tries to run a select
    */
  val readyCheck: DockerReadyChecker = new DockerReadyChecker {
    def apply(c: DCS)(implicit d: DockerCommandExecutor, ec: ExecutionContext): Future[Boolean] =
      database[IO].use(sql"SELECT 1".query[Int].unique.transact(_)).attempt.map(_.isRight).unsafeToFuture()
  }

  val postgres: DockerContainer =
    DockerContainer("postgres")
      .withPorts(5432 -> Some(5432))
      .withReadyChecker(readyCheck.looped(attempts = 100, delay = 100.millis))

  override def dockerContainers: List[DockerContainer] =
    postgres :: super.dockerContainers

  /**
    * This very unsafe query has to be a def because for some reason if it is a val
    * the tests try to connect to the database too early. Good old pure FP.
    */
  private def massTruncate: ConnectionIO[Unit] =
    for {
      ts <- sql"SELECT tablename FROM pg_tables WHERE schemaname = 'public'".query[String].to[List]
      _  <- ts.filterNot(_.startsWith("flyway")).traverse(r => Update(s"TRUNCATE $r CASCADE").run(()))
    } yield ()

  /**
    * Syntax sugar for running tests requiring a transactor,
    * saves us from one layer of ugly unsafeRunSync. Also cleans the DB after running
    */
  protected final implicit class WordSpecStringWrapper(string: String) {
    def inDb(f: Transactor[IO] => Prop)(implicit pos: source.Position): Unit = {
      registerTest(string) {
        database[IO].use { db =>
          for {
            test <- IO.pure(check(f(db))).attempt
            _ <- massTruncate.transact(db)
            res <- IO.fromEither(test)
          } yield res
        }.unsafeRunSync()
      }(pos)
    }
  }

}
