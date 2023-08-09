package it.unibo.citytwin.control_panel

import it.unibo.citytwin.control_panel.actors.ControlPanelActor
import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.Main.args

object Main extends App:
  private val port: Int = if args.length == 0 then 2551 else args(0).toInt
  startup(port = port)(ControlPanelActor())