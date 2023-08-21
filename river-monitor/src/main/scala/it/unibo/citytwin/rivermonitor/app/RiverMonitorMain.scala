package it.unibo.citytwin.rivermonitor.app

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorActor
import it.unibo.citytwin.rivermonitor.model.RiverMonitor

@main def riverMonitorMain(args: String*): Unit =
  if args.length < 5 then
    println("Usage: riverMonitorMain <Port> <riverMonitorName> <x_position> <y_position> [<resources_to_check>...]")
  else {
    val port = args(0).toInt
    val riverMonitorName = args(1)
    val positionX = args(2).toInt
    val positionY = args(3).toInt
    val resourcesToCheck = args.drop(4).toSet
    val riverMonitor = RiverMonitor(riverMonitorName, Point2D[Int](positionX, positionY))
    startup(port)(RiverMonitorActor(riverMonitor, resourcesToCheck))
  }
