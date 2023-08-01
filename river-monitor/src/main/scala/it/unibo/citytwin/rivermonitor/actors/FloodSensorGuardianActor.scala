package it.unibo.citytwin.rivermonitor.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.rivermonitor.model.FloodSensor

object FloodSensorGuardianActor:
  def apply(floodSensor: FloodSensor): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val floodSensorActor = ctx.spawnAnonymous(FloodSensorActor(floodSensor))
      ctx.system.receptionist ! Receptionist.Subscribe(floodSensorService, ctx.self)
      ctx.system.receptionist ! Receptionist.Subscribe(riverMonitorService, ctx.self)
      ctx.system.receptionist ! Receptionist.Subscribe(viewService, ctx.self)

      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case floodSensorService.Listing(listings) => {
          ctx.log.debug("Received floodSensorService")
          listings.foreach(actor => actor ! IsMyZoneRequestFloodSensor(floodSensor.zoneId, floodSensorActor))
          Behaviors.same
        }
        case riverMonitorService.Listing(listings) =>{
          ctx.log.debug("Received riverMonitorService")
          listings.foreach(actor => actor ! IsMyZoneRequestFromFloodSensorToRiverMonitor(floodSensor.zoneId, floodSensorActor))
          Behaviors.same
        }
        case viewService.Listing(listings) =>{
          ctx.log.debug("Received viewService")
          listings.foreach(actor => actor ! UpdateFloodSensor(floodSensor))
          Behaviors.same
        }
        case _ => {
          ctx.log.debug("Received Stop")
          Behaviors.stopped
        }
      }
    }.narrow
