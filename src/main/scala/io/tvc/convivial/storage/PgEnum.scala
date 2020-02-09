package io.tvc.convivial.storage

import cats.instances.either._
import cats.instances.list._
import cats.instances.string._
import cats.syntax.traverse._
import doobie.postgres.implicits._
import scala.reflect.runtime.universe._
import doobie.util.{Get, Put}
import enumeratum._

trait PgEnum[A <: EnumEntry] { self: DoobieEnum[A] with Enum[A] =>

  implicit val put: Put[List[A]] =
    Put[List[String]].contramap(_.map(_.entryName))

  implicit def get(implicit tt: TypeTag[List[A]]): Get[List[A]] =
    Get[List[String]].temap(_.traverse(self.withNameEither).left.map(_.notFoundName))
}
