package it.unibo.citytwin.core.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.ResourceActorCommand
import it.unibo.citytwin.core.model.ResourceState

object DummyResourceActor:
  def apply(
      mainstays: Set[ActorRef[MainstayActorCommand]] = Set()
  ): Behavior[ResourceActorCommand] =
    Behaviors.setup[ResourceActorCommand] { ctx =>
      Behaviors.receiveMessage {
        case AdaptedResourcesStateResponse(
              _: ActorRef[ResourcesFromMainstayResponse],
              resources: Set[ResourceState]
            ) =>
          ctx.log.debug(s"$resources")
          Behaviors.same
      }
    }
