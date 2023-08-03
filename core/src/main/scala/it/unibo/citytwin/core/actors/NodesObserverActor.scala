package it.unibo.citytwin.core.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.model.Resource

trait NodesObserverActorCommand
case class UpdateMainstayNodesState(refs: Set[ActorRef[MainstayActorCommand]])
    extends NodesObserverActorCommand
    with Serializable
case class UpdateResourceNodesState(refs: Set[ActorRef[ResourceActorCommand]])
    extends NodesObserverActorCommand
    with Serializable

object NodesObserverActor:
  def apply(
      mainstay: ActorRef[MainstayActorCommand],
      mainstays: Map[ActorRef[MainstayActorCommand], Boolean] = Map(),
      resources: Map[ActorRef[ResourceActorCommand], Boolean] = Map()
  ): Behavior[NodesObserverActorCommand] =
    Behaviors.setup[NodesObserverActorCommand] { ctx =>
      Behaviors.receiveMessage {
        case UpdateMainstayNodesState(refs: Set[ActorRef[MainstayActorCommand]]) => {
          ctx.log.debug("UpdateMainstaysState")
          val result: Map[ActorRef[MainstayActorCommand], Boolean] =
            mainstays.map((k, _) => (k, false)) ++ refs.map(k => (k, true))
          mainstay ! SetMainstays(result)
          NodesObserverActor(mainstay, result, resources)
        }
        case UpdateResourceNodesState(refs: Set[ActorRef[ResourceActorCommand]]) => {
          ctx.log.debug("UpdateResourcesState")
          val result: Map[ActorRef[ResourceActorCommand], Boolean] =
            resources.map((k, _) => (k, false)) ++ refs.map(k => (k, true))
          mainstay ! UpdateResources(result.map((k, v) => (k, Resource(nodeState = Some(v)))))
          NodesObserverActor(mainstay, mainstays, result)
        }
      }
    }
