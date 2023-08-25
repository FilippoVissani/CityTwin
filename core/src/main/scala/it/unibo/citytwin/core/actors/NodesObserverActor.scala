package it.unibo.citytwin.core.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.model.Resource

/**
  * NodesObserverActorCommand is the trait that defines the messages that can be sent to the NodesObserverActor
  */
trait NodesObserverActorCommand

/**
  * UpdateMainstayNodesState is the message that can be sent to the NodesObserverActor to update the state of a set of Mainstay Actors
  *
  * @param refs the set of Mainstay Actor's refs
  */
case class UpdateMainstayNodesState(refs: Set[ActorRef[MainstayActorCommand]])
    extends NodesObserverActorCommand
    with Serializable

/**
 * UpdateResourceNodesState is the message that can be sent to the NodesObserverActor to update the state of a set of Resource Actors
 * @param refs the set of Resource Actor's refs
 */
case class UpdateResourceNodesState(refs: Set[ActorRef[ResourceActorCommand]])
    extends NodesObserverActorCommand
    with Serializable

/**
 * NodesObserverActor is the actor that sends updates to the Mainstay Actor about the state of the nodes of the cluster
 */
object NodesObserverActor:
  /**
   * Generates new NodesObserverActor.
   * @param mainstay the Mainstay Actor that will receive the updates.
   * @param mainstays the state of the Mainstay Actors.
   * @param resources the state of the Resource Actors.
   * @return the behavior of NodesObserverActor.
   */
  def apply(
      mainstay: ActorRef[MainstayActorCommand],
      mainstays: Map[ActorRef[MainstayActorCommand], Boolean] = Map(),
      resources: Map[ActorRef[ResourceActorCommand], Boolean] = Map()
  ): Behavior[NodesObserverActorCommand] =
    Behaviors.setup[NodesObserverActorCommand] { ctx =>
      ctx.log.debug("Nodes observer started")
      Behaviors.receiveMessage {
        case UpdateMainstayNodesState(refs: Set[ActorRef[MainstayActorCommand]]) => {
          ctx.log.debug("UpdateMainstaysState")
          val result: Map[ActorRef[MainstayActorCommand], Boolean] =
            mainstays.map((k, _) => (k, false)) ++ refs.map(k => (k, true))
          mainstay ! SetMainstays(result.toSet)
          NodesObserverActor(mainstay, result, resources)
        }
        case UpdateResourceNodesState(refs: Set[ActorRef[ResourceActorCommand]]) => {
          ctx.log.debug("UpdateResourcesState")
          val result: Map[ActorRef[ResourceActorCommand], Boolean] =
            resources.map((k, _) => (k, false)) ++ refs.map(k => (k, true))
          mainstay ! UpdateResources(result.map((k, v) => (k, Resource(nodeState = Some(v)))).toSet)
          NodesObserverActor(mainstay, mainstays, result)
        }
      }
    }
