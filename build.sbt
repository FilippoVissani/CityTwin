val scala3Version = "3.3.0"
lazy val akkaVersion = "2.8.3"
lazy val akkaGroup = "com.typesafe.akka"

lazy val root = project
  .in(file("."))
  .settings(
    name := "CityTwin",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      akkaGroup %% "akka-actor-typed" % akkaVersion,
      akkaGroup %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.8",
      akkaGroup %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.16" % Test,
    )
  )
