package it.unibo.citytwin.core.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

trait NodesObserverActorCommand
case class UpdateMainstayNodesState(refs: Set[ActorRef[MainstayActorCommand]]) extends NodesObserverActorCommand with Serializable
case class UpdateResourceNodesState(refs: Set[ActorRef[ResourceActorCommand]]) extends NodesObserverActorCommand with Serializable

object NodesObserverActor:
  def apply(mainstay: ActorRef[MainstayActorCommand],
            mainstays: Map[ActorRef[MainstayActorCommand], Boolean] = Map(),
            resources: Map[ActorRef[ResourceActorCommand], Boolean] = Map()): Behavior[NodesObserverActorCommand] =
    Behaviors.setup[NodesObserverActorCommand] { ctx =>
      Behaviors.receiveMessage {
        case SetMainstayActors(refs: Set[ActorRef[MainstayActorCommand]]) => {
          ctx.log.debug("UpdateMainstaysState")
          val result: Map[ActorRef[MainstayActorCommand], Boolean] = mainstays.map((k, _) => (k, false)) ++ refs.iterator.map(k => (k, true))
          mainstay ! SetMainstayActors(result)
          NodesObserverActor(mainstay, result, resources)
        }
        case SetResourceNodesState(refs: Set[ActorRef[ResourceActorCommand]]) => {
          ctx.log.debug("UpdateResourcesState")
          val result: Map[ActorRef[ResourceActorCommand], Boolean] = resources.map((k, _) => (k, false)) ++ refs.iterator.map(k => (k, true))
          mainstay ! SetResourceNodesState(result)
          NodesObserverActor(mainstay, mainstays, result)
        }
      }
    }

