package it.unibo.citytwin.core

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.actors.{MainstayActor, MainstayActorCommand}

@main def main(args: String*): ActorSystem[MainstayActorCommand] =
  startup(MainstayActor())
