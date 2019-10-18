package io.tvc.convivial.storage

import java.util.concurrent.Executors.newFixedThreadPool

import cats.effect.{Blocker, ContextShift, Effect, Resource, Sync}
import cats.syntax.functor._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.util.transactor.Transactor
import javax.sql.DataSource
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext.fromExecutor

object Postgres {

  case class Config(
    jdbcUrl: String,
    username: String,
    password: String
  )

  private def connect[F[_]: Sync](config: Config): Resource[F, DataSource] =
    Resource(
      Sync[F].delay {
        val hikariConfig = new HikariConfig
        hikariConfig.setJdbcUrl(config.jdbcUrl)
        hikariConfig.setUsername(config.username)
        hikariConfig.setPassword(config.password)
        val source = new HikariDataSource(hikariConfig)
        source -> Sync[F].delay(source.close())
      }
    )

  private def migrate[F[_]: Sync](ds: DataSource): F[Unit] =
    Sync[F].delay(Flyway.configure().dataSource(ds).load().migrate()).as(())

  def transactor[F[_]: Effect: ContextShift](config: Config): Resource[F, Transactor[F]] =
    for {
      blocker <- Blocker.apply
      dataSource <- connect[F](config)
      _ <- Resource.liftF(migrate(dataSource))
      connectEc = fromExecutor(newFixedThreadPool(2))
    } yield Transactor.fromDataSource[F].apply(dataSource, connectEc, blocker)
}
