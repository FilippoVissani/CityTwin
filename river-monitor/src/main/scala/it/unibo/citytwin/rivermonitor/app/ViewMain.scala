package it.unibo.citytwin.rivermonitor.app

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.rivermonitor.actors.view.ViewActor

@main def viewMain(args: String*): Unit =
  if args.length < 5 then
    println("Usage: viewMain <Port> <viewName> <width> <height> [<resources_to_check>...]")
  else {
    val port = args(0).toInt
    val viewName = args(1)
    val width = args(2).toInt
    val height = args(3).toInt
    val resourcesToCheck = args.drop(4).toSet
    startup(port)(ViewActor(viewName, resourcesToCheck, width, height))
  }

