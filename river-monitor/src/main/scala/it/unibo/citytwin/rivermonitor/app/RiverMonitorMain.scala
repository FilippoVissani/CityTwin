package it.unibo.citytwin.rivermonitor.app

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorActor
import it.unibo.citytwin.rivermonitor.model.RiverMonitor

@main def riverMonitorMain(args: String*): Unit =
  val floodSensorName  = "floodSensor1"
  val riverMonitorName = "riverMonitor1"
  val viewName         = "view1"

  val rmResourcesToCheck = Set[String](floodSensorName, viewName)
  val riverMonitor       = RiverMonitor(riverMonitorName, Point2D[Int](0, 0))
  val port: Int          = if args.isEmpty then 2551 else args(0).toInt
  startup(port)(RiverMonitorActor(riverMonitor, rmResourcesToCheck))
