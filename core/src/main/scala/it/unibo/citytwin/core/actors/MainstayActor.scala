package it.unibo.citytwin.core.actors

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.Serializable
import it.unibo.citytwin.core.model.Resource

import scala.collection.immutable.Set

trait MainstayActorCommand
case class AskResourcesState(
    replyTo: ActorRef[ResourceActorCommand],
    names: Set[String]
) extends MainstayActorCommand
    with Serializable
case class SetResourceState(
    ref: ActorRef[ResourceActorCommand],
    resource: Resource
) extends MainstayActorCommand
    with Serializable
case class SetMainstayActors(nodes: Map[ActorRef[MainstayActorCommand], Boolean])
    extends MainstayActorCommand
    with Serializable

case class SetResourceNodesState(nodes: Map[ActorRef[ResourceActorCommand], Boolean])
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
            resources.values.filter(x => names.contains(x.name)).toSet
          )
          Behaviors.same
        }
        case SetResourceState(
              ref: ActorRef[ResourceActorCommand],
              resource: Resource
            ) => {
          ctx.log.debug("SetResourceState")
          mainstays.foreach((k, _) => k ! SetResourceState(ref, resource))
          MainstayActor(mainstays, resources + (ref -> resource))
        }
        case SetResourceNodesState(nodes: Map[ActorRef[ResourceActorCommand], Boolean]) => {
          ctx.log.debug("SetResourceNodesState")
          val result: Map[ActorRef[ResourceActorCommand], Resource] = for
            (k, v)   <- resources
            (k2, v2) <- nodes
          yield if k == k2 then (k, v.copy(nodeState = v2)) else (k2, Resource(nodeState = v2))
          MainstayActor(mainstays, result)
        }
        case SetMainstayActors(nodes: Map[ActorRef[MainstayActorCommand], Boolean]) => {
          ctx.log.debug("SetMainstayActors")
          MainstayActor(nodes, resources)
        }
      }
    }
