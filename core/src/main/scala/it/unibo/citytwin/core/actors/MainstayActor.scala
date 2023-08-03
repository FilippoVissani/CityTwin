package it.unibo.citytwin.core.actors

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource

import scala.collection.immutable.{Map, Set}

trait MainstayActorCommand
case class AskResourcesState(
    replyTo: ActorRef[ResourceActorCommand],
    names: Set[String]
) extends MainstayActorCommand
    with Serializable
case class UpdateResources(
                            update: Map[ActorRef[ResourceActorCommand], Resource]
) extends MainstayActorCommand
    with Serializable
case class SetMainstays(nodes: Map[ActorRef[MainstayActorCommand], Boolean])
    extends MainstayActorCommand
    with Serializable

object MainstayActor:
  val mainstayService: ServiceKey[MainstayActorCommand] =
    ServiceKey[MainstayActorCommand]("mainstayService")

  def apply(
      mainstays: Map[ActorRef[MainstayActorCommand], Boolean] = Map(),
      resources: Map[ActorRef[ResourceActorCommand], Resource] = Map()
  ): Behavior[MainstayActorCommand] =
    Behaviors.setup[MainstayActorCommand] { ctx =>
      Behaviors.receiveMessage {
        case AskResourcesState(
              replyTo: ActorRef[ResourceActorCommand],
              names: Set[String]
            ) => {
          ctx.log.debug("AskResourceState")
          replyTo ! ResponseResourceState(
            resources.values.filter(x => x.name.isDefined).filter(x => names.contains(x.name.get)).toSet
          )
          Behaviors.same
        }
        case UpdateResources(update: Map[ActorRef[ResourceActorCommand], Resource]) => {
          ctx.log.debug("UpdateResources")
          mainstays.foreach((k, _) => k ! UpdateResources(resources))
          val result: Map[ActorRef[ResourceActorCommand], Resource] = resources.map((k, v) => if update.contains(k) then (k, v.merge(update(k))) else (k, v)) ++ (update -- resources.keys)
          MainstayActor(mainstays, result)
        }
        case SetMainstays(nodes: Map[ActorRef[MainstayActorCommand], Boolean]) => {
          ctx.log.debug("SetMainstays")
          MainstayActor(nodes, resources)
        }
      }
    }
