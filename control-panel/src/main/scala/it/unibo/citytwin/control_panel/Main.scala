package it.unibo.citytwin.control_panel

import akka.actor.typed.ActorSystem
import it.unibo.citytwin.control_panel.actors.ControlPanelActor
import it.unibo.citytwin.control_panel.actors.ControlPanelActorCommand
import it.unibo.citytwin.core.ActorSystemStarter.startup

@main def main(args: String*): ActorSystem[ControlPanelActorCommand] =
  val port: Int = if args.isEmpty then 2551 else args(0).toInt
  val citySize: (Double, Double) = if args.isEmpty then (0, 0) else (args(1).toDouble, args(2).toDouble)
  val cityMap: String = if args.isEmpty then "" else args(3)
  val persistenceServiceHost: String = if args.isEmpty then "127.0.0.1" else args(4)
  val persistenceServicePort: String = if args.isEmpty then "8080" else args(5)
  startup(port)(ControlPanelActor(citySize, cityMap, persistenceServiceHost, persistenceServicePort))
