package it.unibo.citytwin.core

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}

object ActorSystemStarter:
  def startup[X](file: String = "cluster", port: Int)(root: => Behavior[X]): ActorSystem[X] =
    // Override the configuration of the port
    val config: Config = ConfigFactory
      .parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load(file))
    // Create an Akka system
    ActorSystem(root, "ClusterSystem", config)
