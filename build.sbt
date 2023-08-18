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
  "org.scalatest" %% "scalatest" % "3.2.16" % Test
)
lazy val scalaSwing = "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
lazy val mongoDBDriver = ("org.mongodb.scala" %% "mongo-scala-driver" % "4.10.2").cross(CrossVersion.for3Use2_13)
lazy val playJSON = ("com.typesafe.play" %% "play-json" % "2.9.4").cross(CrossVersion.for3Use2_13)
lazy val commonSettings = Seq(
  libraryDependencies := commonDependencies,
)

lazy val core = project
  .in(file("core"))
  .settings(
    commonSettings,
    name := "core",
    libraryDependencies ++= Seq(mongoDBDriver, playJSON),
    Compile / packageBin / mainClass := Some("it.unibo.citytwin.core.Main"),
  ).enablePlugins(JavaAppPackaging)

lazy val controlPanel = project
  .in(file("control-panel"))
  .settings(
    commonSettings,
    name := "control-panel",
    libraryDependencies += scalaSwing,
    Compile / packageBin / mainClass := Some("it.unibo.citytwin.control_panel.Main"),
  ).dependsOn(core).enablePlugins(JavaAppPackaging)

lazy val riverMonitor = project
  .in(file("river-monitor"))
  .settings(
    commonSettings,
    name := "river-monitor",
    libraryDependencies += scalaSwing,
  ).dependsOn(core).enablePlugins(JavaAppPackaging)