package it.unibo.citytwin.rivermonitor.app

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.citytwin.core.actors.MainstayActor
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.floodsensor.FloodSensorActor
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorActor
import it.unibo.citytwin.rivermonitor.actors.view.ViewActor
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}

object Main:

  @main def inizia(): Unit =
    val floodSensorName = "floodSensor1"
    val riverMonitorName ="riverMonitor1"
    val viewName = "view1"

    val floodSensor = FloodSensor(floodSensorName, Point2D[Int](0, 0))
    startup(port = 2551)(FloodSensorActor(floodSensor))

    val rmResourcesToCheck = Set[String](floodSensorName, viewName)
    val riverMonitor = RiverMonitor(riverMonitorName, Point2D[Int](0, 0))
    startup(port = 2552)(RiverMonitorActor(riverMonitor, rmResourcesToCheck))

    startup(port = 2553)(MainstayActor())

    val width = 600
    val height = 200
    val vResourcesToCheck = Set[String](riverMonitorName)
    startup(port = 2554)(ViewActor(viewName, vResourcesToCheck, width, height))

  private def startup[X](file: String = "cluster", port: Int)(root: => Behavior[X]): ActorSystem[X] =
    // Override the configuration of the port
    val config: Config = ConfigFactory
      .parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load(file))
    // Create an Akka system
    ActorSystem(root, "ClusterSystem", config)
