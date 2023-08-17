package it.unibo.citytwin.rivermonitor.app

import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.rivermonitor.actors.view.ViewActor

@main def viewMain(args: String*): Unit =
  val riverMonitorName = "riverMonitor1"
  val viewName         = "view1"

  val width             = 600
  val height            = 200
  val vResourcesToCheck = Set[String](riverMonitorName)
  val port: Int         = if args.isEmpty then 2551 else args(0).toInt
  startup(port)(ViewActor(viewName, vResourcesToCheck, width, height))
