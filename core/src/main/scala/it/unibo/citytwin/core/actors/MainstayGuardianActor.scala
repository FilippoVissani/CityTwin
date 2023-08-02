package it.unibo.citytwin.core.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.MainstayActor.mainstayService
import it.unibo.citytwin.core.actors.ResourceActor.resourceService

object MainstayGuardianActor:

  def apply(): Behavior[Nothing] =
    Behaviors
      .setup[Receptionist.Listing] { ctx =>
        val mainstayActor = ctx.spawnAnonymous(MainstayActor())
        ctx.system.receptionist ! Receptionist.Subscribe(
          mainstayService,
          ctx.self
        )
        ctx.system.receptionist ! Receptionist.Subscribe(
          resourceService,
          ctx.self
        )
        Behaviors.receiveMessagePartial[Receptionist.Listing] {
          case mainstayService.Listing(listings) => {
            ctx.log.debug("Received mainstayService")
            mainstayActor ! SetMainstayActors(listings)
            Behaviors.same
          }
          case resourceService.Listing(listings) => {
            ctx.log.debug("Received mainstayService")
            listings.foreach(x => mainstayActor ! SetResourceState(x, None))
            Behaviors.same
          }
          case _ => Behaviors.stopped
        }
      }
      .narrow
