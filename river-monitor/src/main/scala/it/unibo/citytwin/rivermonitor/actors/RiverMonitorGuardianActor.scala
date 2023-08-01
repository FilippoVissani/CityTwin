package it.unibo.citytwin.rivermonitor.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.rivermonitor.model.{RiverMonitor, Zone}

object RiverMonitorGuardianActor:

  def apply(riverMonitor: RiverMonitor, zone: Zone): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val riverMonitorActor = ctx.spawnAnonymous(RiverMonitorActor(riverMonitor, zone))
      ctx.system.receptionist ! Receptionist.Subscribe(viewService, ctx.self)

      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case viewService.Listing(listings) =>{
          ctx.log.debug("Received viewService")
          riverMonitorActor ! SetViews(listings)
          Behaviors.same
        }
        case _ => Behaviors.stopped
      }
    }.narrow
