ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "3.3.0"
ThisBuild / organization := "it.unibo"
ThisBuild / scalacOptions ++= Seq("-feature", "-deprecation")

fork := true

lazy val akkaVersion = "2.8.3"
lazy val akkaHttpVersion = "10.5.2"
lazy val akkaGroup = "com.typesafe.akka"
lazy val commonDependencies = Seq(
  akkaGroup %% "akka-actor-typed" % akkaVersion,
  akkaGroup %% "akka-cluster-typed" % akkaVersion,
  akkaGroup %% "akka-serialization-jackson" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.8",
  akkaGroup %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.16" % Test
)
lazy val scalaSwing = "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
lazy val jFreeChart = "org.jfree" % "jfreechart" % "1.5.4"
lazy val upickleJson = "com.lihaoyi" %% "upickle" % "3.1.2"
lazy val akkaHttp = akkaGroup %% "akka-http" % akkaHttpVersion
lazy val playJson = ("com.typesafe.play" %% "play-json" % "2.9.4").cross(CrossVersion.for3Use2_13)
lazy val commonSettings = Seq(
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  libraryDependencies := commonDependencies,
  scriptClasspath := Seq("*"),
)

lazy val core = project
  .in(file("core"))
  .settings(
    commonSettings,
    name := "core",
    libraryDependencies ++= Seq(akkaHttp, playJson),
    Compile / packageBin / mainClass := Some("it.unibo.citytwin.core.Main"),
  ).enablePlugins(JavaAppPackaging)

lazy val controlPanel = project
  .in(file("control-panel"))
  .settings(
    commonSettings,
    name := "control-panel",
    libraryDependencies ++= Seq(scalaSwing, jFreeChart, akkaHttp),
    Compile / packageBin / mainClass := Some("it.unibo.citytwin.control_panel.Main"),
  ).dependsOn(core).enablePlugins(JavaAppPackaging)

lazy val riverMonitor = project
  .in(file("river-monitor"))
  .settings(
    commonSettings,
    name := "river-monitor",
    libraryDependencies ++= Seq(upickleJson,scalaSwing),
  ).dependsOn(core).enablePlugins(JavaAppPackaging)

lazy val airQualityMonitor = project
  .in(file("air-quality-monitor"))
  .settings(
    commonSettings,
    name := "air-quality-monitor",
    libraryDependencies ++= Seq(upickleJson,scalaSwing),
  ).dependsOn(core).enablePlugins(JavaAppPackaging)

lazy val acidRainMonitor = project
  .in(file("acid-rain-monitor"))
  .settings(
    commonSettings,
    name := "acid-rain-monitor",
    libraryDependencies ++= Seq(upickleJson, scalaSwing)
  ).dependsOn(core).enablePlugins(JavaAppPackaging)

lazy val noisePollutionMonitor = project
  .in(file("noise-pollution-monitor"))
  .settings(
    commonSettings,
    name := "noise-pollution-monitor",
    libraryDependencies ++= Seq(upickleJson, scalaSwing)
  ).dependsOn(core).enablePlugins(JavaAppPackaging)
