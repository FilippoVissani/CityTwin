package it.unibo.citytwin.core

import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}

object ActorSystemStarter:
  def startup[X](root: => Behavior[X]): ActorSystem[X] =
    val config: Config = ConfigFactory.load()
    val clusterName = config.getString("clustering.cluster.name")
    // Create an Akka system
    ActorSystem(root, clusterName)
