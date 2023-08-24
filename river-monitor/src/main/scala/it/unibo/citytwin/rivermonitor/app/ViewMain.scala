package it.unibo.citytwin.rivermonitor.app

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.rivermonitor.actors.view.ViewActor

/** Main entry point for the View application. The application creates and starts a ViewActor based
  * on the provided arguments.
  */
@main def viewMain(args: String*): Unit =
  if args.length != 5 then
    println("Usage: viewMain <Port> <viewName> <width> <height> <riverMonitor_to_check>")
  else {
    // Parse command-line arguments
    val port                = args(0).toInt
    val viewName            = args(1)
    val width               = args(2).toInt
    val height              = args(3).toInt
    val riverMonitorToCheck = args.drop(4).toSet
    // Start the actor system
    startup(port)(ViewActor(viewName, riverMonitorToCheck, width, height))
  }
