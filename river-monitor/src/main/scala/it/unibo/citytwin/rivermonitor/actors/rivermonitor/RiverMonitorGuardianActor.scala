package it.unibo.citytwin.rivermonitor.actors.rivermonitor

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.MainstayActor.mainstayService
import it.unibo.citytwin.core.actors.SetMainstayActorsToResourceActor
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.SetResourceActor
import it.unibo.citytwin.rivermonitor.actors.rivermonitor.RiverMonitorActor
import it.unibo.citytwin.rivermonitor.model.{RiverMonitor, Zone}

object RiverMonitorGuardianActor:

  def apply(riverMonitor: RiverMonitor): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val riverMonitorActor = ctx.spawnAnonymous(RiverMonitorActor(riverMonitor))
      val resourceRiverMonitorActor = ctx.spawnAnonymous(ResourceRiverMonitorActor(riverMonitorActor))
      riverMonitorActor ! SetResourceActor(resourceRiverMonitorActor)

      ctx.system.receptionist ! Receptionist.Subscribe(mainstayService, ctx.self)

      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case mainstayService.Listing(listings) => {
          ctx.log.debug("Received mainstayService")
          //tell all mainstayActors to resourceRiverMonitorActor
          resourceRiverMonitorActor ! SetMainstayActorsToResourceActor(listings)
          Behaviors.same
        }
        case _ => {
          ctx.log.debug(s"Unexpected message. The actor is being stopped")
          Behaviors.stopped
        }
      }
    }.narrow
