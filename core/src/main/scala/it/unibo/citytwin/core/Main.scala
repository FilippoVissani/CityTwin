package it.unibo.citytwin.core

import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.citytwin.core.actors.MainstayActor

@main def main(port: String = "2551"): Unit =
  val config: Config = ConfigFactory
    .parseString(s"""akka.remote.artery.canonical.port=$port""")
    .withFallback(ConfigFactory.load("cluster"))
  ActorSystem(MainstayActor(), "ClusterSystem", config)
