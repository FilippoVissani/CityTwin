package it.unibo.citytwin.rivermonitor.app

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.actors.MainstayActor
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.floodsensor.FloodSensorActor
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorActor
import it.unibo.citytwin.rivermonitor.actors.view.ViewActor
import it.unibo.citytwin.rivermonitor.model.{FloodSensor, RiverMonitor}

@main def main(args: String*): Unit =
  val floodSensorName = "floodSensor1"
  val riverMonitorName ="riverMonitor1"
  val viewName = "view1"

  val floodSensor = FloodSensor(floodSensorName, Point2D[Int](0, 0))
  startup()(FloodSensorActor(floodSensor))

  val rmResourcesToCheck = Set[String](floodSensorName, viewName)
  val riverMonitor = RiverMonitor(riverMonitorName, Point2D[Int](0, 0))
  startup()(RiverMonitorActor(riverMonitor, rmResourcesToCheck))
  
  val width = 600
  val height = 200
  val vResourcesToCheck = Set[String](riverMonitorName)
  startup()(ViewActor(viewName, vResourcesToCheck, width, height))
