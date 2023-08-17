package it.unibo.citytwin.rivermonitor.app

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.floodsensor.FloodSensorActor
import it.unibo.citytwin.rivermonitor.model.FloodSensor

@main def floodSensorMain(args: String*): Unit =
  val floodSensorName = "floodSensor1"
  val riverMonitorName ="riverMonitor1"
  val viewName = "view1"

  val floodSensor = FloodSensor(floodSensorName, Point2D[Int](0, 0))
  startup(FloodSensorActor(floodSensor))