package it.unibo.citytwin.rivermonitor.app

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.model.Point2D
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorActor
import it.unibo.citytwin.rivermonitor.model.RiverMonitor
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.Safe

/** Main entry point for the River Monitor application. The application creates and starts a
  * RiverMonitorActor based on the provided arguments.
  */
@main def riverMonitorMain(args: String*): Unit =
  if args.length < 6 then
    println(
      "Usage: riverMonitorMain <Port> <riverMonitorName> <threshold> <x_position> <y_position> [<resources_to_check>...]"
    )
  else {
    // Parse command-line arguments
    val port             = args(0).toInt
    val riverMonitorName = args(1)
    val threshold        = args(2).toFloat
    val positionX        = args(3).toInt
    val positionY        = args(4).toInt
    val resourcesToCheck = args.drop(5).toSet
    // Create a RiverMonitor instance
    val riverMonitor =
      RiverMonitor(riverMonitorName, Point2D[Int](positionX, positionY), Safe, threshold)
    // Start the actor system
    startup(port)(RiverMonitorActor(riverMonitor, resourcesToCheck))
  }
