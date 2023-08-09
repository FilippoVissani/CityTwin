package it.unibo.citytwin.rivermonitor.actors.floodsensor

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.MainstayActor.mainstayService
import it.unibo.citytwin.core.actors.{ResourceActor, SetMainstayActorsToResourceActor}
import it.unibo.citytwin.rivermonitor.actors.floodsensor.{FloodSensorActor, ResourceFloodSensorActor, SetResourceActor}
import it.unibo.citytwin.rivermonitor.model.FloodSensor

object FloodSensorGuardianActor:
  def apply(floodSensor: FloodSensor): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val floodSensorActor = ctx.spawnAnonymous(FloodSensorActor(floodSensor))
      val resourceFloodSensorActor = ctx.spawnAnonymous(ResourceFloodSensorActor(floodSensorActor))
      floodSensorActor ! SetResourceActor(resourceFloodSensorActor)

      ctx.system.receptionist ! Receptionist.Subscribe(mainstayService, ctx.self)

      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case mainstayService.Listing(listings) => {
          ctx.log.debug("Received mainstayService")
          //tell all mainstayActors to resourceFloodSensorActor
          resourceFloodSensorActor ! SetMainstayActorsToResourceActor(listings)
          Behaviors.same
        }
        case _ => {
          ctx.log.debug(s"Unexpected message. The actor is being stopped")
          Behaviors.stopped
        }
      }
    }.narrow