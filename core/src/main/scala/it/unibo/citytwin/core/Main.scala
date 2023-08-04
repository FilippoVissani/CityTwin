package it.unibo.citytwin.core

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.citytwin.core.actors.MainstayActor

object Main extends App:
  private val config: Config = ConfigFactory
    .parseString(s"""akka.remote.artery.canonical.port=2551""")
    .withFallback(ConfigFactory.load("cluster"))
  ActorSystem(MainstayActor(), "ClusterSystem", config)
