package it.unibo.citytwin.core

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.citytwin.core.ActorSystemStarter.startup
import it.unibo.citytwin.core.actors.MainstayActor

object Main extends App:
  private val port: Int = if args.length == 0 then 2551 else args(0).toInt
  startup(port = port)(MainstayActor())
