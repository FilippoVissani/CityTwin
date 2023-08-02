package it.unibo.citytwin.rivermonitor.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

object ViewGuardianActor:
  def apply(zoneId: Int,
            width: Int,
            height: Int): Behavior[Nothing] = 
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val viewActor = ctx.spawnAnonymous(Behaviors.setup(new ViewActor(_, zoneId, width, height)))
      ctx.system.receptionist ! Receptionist.subscribe(riverMonitorService, ctx.self)

      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case riverMonitorService.Listing(listings) =>{
          ctx.log.debug("Received riverMonitorService")
          listings.foreach(actor => actor ! IsMyZoneRequestFromViewToRiverMonitor(zoneId, viewActor))
          Behaviors.same
        }
        case _ => Behaviors.stopped
      }
    }.narrow

