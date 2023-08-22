package it.unibo.citytwin.core

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.actors.{MainstayActor, MainstayActorCommand}

@main def main(args: String*): ActorSystem[MainstayActorCommand] =
  val port: Int = if args.isEmpty then 2551 else args(0).toInt
  val persistenceServiceHost: String = if args.isEmpty then "127.0.0.1" else args(1)
  val persistenceServicePort: String = if args.isEmpty then "8080" else args(2)
  startup(port)(MainstayActor(persistenceServiceHost, persistenceServicePort))
