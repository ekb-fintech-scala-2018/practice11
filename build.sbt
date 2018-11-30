name := "crwlr"

version := "0.1"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-feature",
  "-language:higherKinds"
)

resolvers += Resolver.sonatypeRepo("snapshots")

updateOptions := updateOptions.value.withLatestSnapshots(false)

addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.18"

libraryDependencies += "io.monix" %% "monix" % "3.0.0-RC2"

libraryDependencies += "com.typesafe" % "config" % "1.3.2"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.18"

libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.5"

val http4sVersion = "0.20.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion
)

val circeVersion = "0.10.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val slickVersion = "3.2.3"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.h2database" % "h2" % "1.4.197"
)

val doobieVersion = "0.6.0"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-h2" % doobieVersion
)
