name := "convivial"

version := "0.1"

scalaVersion := "2.12.10"

val http4sVersion = "0.21.0-M5"
val catsEffectVersion = "2.0.0"
val catsVersion = "2.0.0"
val circeVersion = "0.12.2"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "com.google.guava" % "guava" % "28.1-jre",

  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsVersion,

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "com.zaxxer" % "HikariCP" % "3.4.1",
  "org.flywaydb" % "flyway-core" % "6.0.6",
  "org.tpolecat" %% "doobie-core" % "0.8.4",
  "org.tpolecat" %% "doobie-postgres"  % "0.8.4",
  "dev.profunktor" %% "redis4cats-effects" % "0.9.0",

  "org.typelevel" %% "claimant" % "0.1.0" % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.1" % Test
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
