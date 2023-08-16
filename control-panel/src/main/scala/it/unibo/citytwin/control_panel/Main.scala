package it.unibo.citytwin.control_panel

import akka.actor.typed.ActorSystem
import it.unibo.citytwin.control_panel.actors.{ControlPanelActor, ControlPanelActorCommand}
import it.unibo.citytwin.core.ActorSystemStarter.startup

@main def main(args: String*): ActorSystem[ControlPanelActorCommand] =
  startup(ControlPanelActor())
