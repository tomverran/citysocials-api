name := "convivial"

version := "0.1"

scalaVersion := "2.13.1"

val http4sVersion = "0.21.0-RC4"
val catsEffectVersion = "2.0.0"
val catsVersion = "2.1.0"
val circeVersion = "0.12.3"

scalacOptions += "-Ymacro-annotations"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "com.google.guava" % "guava" % "28.1-jre",

  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsVersion,
  "io.chrisdavenport" %% "cats-effect-time" % "0.1.1",

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % "0.12.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "com.zaxxer" % "HikariCP" % "3.4.1",
  "org.flywaydb" % "flyway-core" % "6.0.6",
  "org.tpolecat" %% "doobie-core" % "0.8.8",
  "org.tpolecat" %% "doobie-postgres"  % "0.8.8",
  "dev.profunktor" %% "redis4cats-effects" % "0.9.0",

  "com.beachape" %% "enumeratum" % "1.5.15",
  "com.beachape" %% "enumeratum-circe" % "1.5.22",
  "com.beachape" %% "enumeratum-doobie" % "1.5.17",

  "org.typelevel" %% "claimant" % "0.1.3" % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "org.scalatestplus" %% "scalacheck-1-14" % "3.1.0.1" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.1" % Test,
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.9" % Test,
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.9" % Test
)

enablePlugins(JavaAppPackaging)
