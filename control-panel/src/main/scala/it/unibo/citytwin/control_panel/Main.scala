package it.unibo.citytwin.control_panel

import akka.actor.typed.ActorSystem
import it.unibo.citytwin.control_panel.actors.{ControlPanelActor, ControlPanelActorCommand}
import it.unibo.citytwin.core.ActorSystemStarter.startup

@main def main(args: String*): ActorSystem[ControlPanelActorCommand] =
  val port: Int = if args.isEmpty then 2551 else args(0).toInt
  val citySize: (Double, Double) = if args.isEmpty then (0, 0) else (args(1).toDouble, args(2).toDouble)
  startup(port)(ControlPanelActor(citySize))
