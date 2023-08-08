package it.unibo.citytwin.rivermonitor.app

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.citytwin.core.actors.MainstayActor
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.floodsensor.FloodSensorGuardianActor
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorGuardianActor
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}

object Main:

  @main def inizia(): Unit =
    val floodSensorName = "floodSensor1"
    val floodSensor = FloodSensor(floodSensorName, Point2D[Int](0, 0))
    startup(port = 2551)(FloodSensorGuardianActor(floodSensor))

    val riverMonitorName ="riverMonitor1"
    val sensorsToCheck = Set[String](floodSensorName)
    val riverMonitor = RiverMonitor(riverMonitorName, sensorsToCheck, Point2D[Int](0, 0))
    startup(port = 2552)(RiverMonitorGuardianActor(riverMonitor))

    startup(port = 2553)(MainstayActor())

  private def startup[X](file: String = "cluster", port: Int)(root: => Behavior[X]): ActorSystem[X] =
    // Override the configuration of the port
    val config: Config = ConfigFactory
      .parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load(file))
    // Create an Akka system
    ActorSystem(root, "ClusterSystem", config)
