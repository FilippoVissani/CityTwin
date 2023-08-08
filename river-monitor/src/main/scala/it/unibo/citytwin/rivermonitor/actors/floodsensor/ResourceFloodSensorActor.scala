package it.unibo.citytwin.rivermonitor.actors.floodsensor

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.citytwin.core.actors.ResourceActor.resourceService
import it.unibo.citytwin.core.actors.*

object ResourceFloodSensorActor :
  def apply(floodSensorActor: ActorRef[FloodSensorActorCommand],
            mainstayActors: Set[ActorRef[MainstayActorCommand]] = Set()): Behavior[ResourceActorCommand] =
    Behaviors.setup[ResourceActorCommand] { ctx =>
      ctx.system.receptionist ! Receptionist.Register(resourceService, ctx.self)
      Behaviors.receiveMessage {
        case AdaptedResourcesStateResponse(resources) => {
          ctx.log.debug("Received ResponseResourceState")
          //do nothing, this is a sensor that don't ask for other resource status
          Behaviors.same
        }
        case SetMainstayActorsToResourceActor(mainstayActors) => {
          ctx.log.debug("Received SetMainstayActorsToResource")
          ResourceFloodSensorActor(floodSensorActor, mainstayActors)
        }
        case ResourceChanged(resource) => {
          ctx.log.debug("Received ResourceChanged")
          mainstayActors.foreach(mainstay => mainstay ! UpdateResources(Map(ctx.self -> resource).toSet))
          Behaviors.same
        }
        case _ => {
          Behaviors.stopped
        }
      }
    }
