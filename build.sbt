ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.0"
ThisBuild / organization := "it.unibo"

lazy val akkaVersion = "2.8.3"
lazy val akkaGroup = "com.typesafe.akka"
lazy val commonDependencies = Seq(
  akkaGroup %% "akka-actor-typed" % akkaVersion,
  akkaGroup %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.8",
  akkaGroup %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.16" % Test,
)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "CityTwin-core",
    libraryDependencies ++= commonDependencies
  )

lazy val riverMonitor = project
  .in(file("river-monitor"))
  .settings(
    name := "CityTwin-river-monitor",
    libraryDependencies ++= commonDependencies
  )