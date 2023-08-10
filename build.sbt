ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "3.3.0"
ThisBuild / organization := "it.unibo"

fork := true

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
lazy val commonDependenciesGUI = commonDependencies ++ Seq("org.scala-lang.modules" %% "scala-swing" % "3.0.0")

lazy val core = project
  .in(file("core"))
  .settings(
    name := "core",
    libraryDependencies ++= commonDependencies,
    Compile / packageBin / mainClass := Some("it.unibo.citytwin.core.Main"),
  ).enablePlugins(JavaAppPackaging)

lazy val controlPanel = project
  .in(file("control-panel"))
  .settings(
    name := "control-panel",
    libraryDependencies ++= commonDependenciesGUI,
    Compile / packageBin / mainClass := Some("it.unibo.citytwin.control_panel.Main"),
  ).dependsOn(core).enablePlugins(JavaAppPackaging)

lazy val riverMonitor = project
  .in(file("river-monitor"))
  .settings(
    name := "river-monitor",
    libraryDependencies ++= commonDependencies
  )