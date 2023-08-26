package it.unibo.citytwin.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/** ActorSystemStarter is the object that starts an Akka Actor System
  */
object ActorSystemStarter:
  /** startup is the method that starts an Akka Actor System
    * @param port
    *   the port of the Actor System
    * @param root
    *   the root behavior of the Actor System
    * @tparam X
    *   the type of the root behavior
    * @return
    *   the Actor System
    */
  def startup[X](port: Int)(root: => Behavior[X]): ActorSystem[X] =
    // Override the configuration of the port
    val config: Config = ConfigFactory
      .parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load())
    // Create an Akka system
    ActorSystem(root, "ClusterSystem", config)
