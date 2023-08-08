package it.unibo.citytwin.control_panel.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.{AdaptedResourcesStateResponse, ResourceActorCommand}
import it.unibo.citytwin.core.model.Resource

object ResourceActor:
  def apply(): Behavior[ResourceActorCommand] =
    Behaviors.setup[ResourceActorCommand] { ctx =>
      Behaviors.receiveMessage {
        case AdaptedResourcesStateResponse(resources: Set[Resource]) => {
          ctx.log.debug("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
          Behaviors.same
        }
      }
    }
