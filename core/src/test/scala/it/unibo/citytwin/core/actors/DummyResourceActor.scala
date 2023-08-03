package it.unibo.citytwin.core.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.{ResourceActorCommand, ResponseResourceState}
import it.unibo.citytwin.core.model.Resource

object DummyResourceActor:
  def apply(): Behavior[ResourceActorCommand] =
    Behaviors.setup[ResourceActorCommand] { ctx =>
      Behaviors.receiveMessage {
        case ResponseResourceState(resources: Set[Resource]) => {
          ctx.log.debug(s"$resources")
          Behaviors.same
        }
      }
    }
