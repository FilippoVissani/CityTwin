package it.unibo.citytwin.core.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.MainstayActor.mainstayService

object ResourceGuardianActor:
  def apply(): Behavior[Nothing] =
    Behaviors
      .setup[Receptionist.Listing] { ctx =>
        val resourceActor = ctx.spawnAnonymous(ResourceActor())
        ctx.system.receptionist ! Receptionist.Subscribe(
          mainstayService,
          ctx.self
        )
        Behaviors.receiveMessagePartial[Receptionist.Listing] {
          case mainstayService.Listing(listings) => {
            ctx.log.debug("Received mainstayService")
            resourceActor ! SetMainstayActorsToResourceActor(listings)
            Behaviors.same
          }
          case _ => Behaviors.stopped
        }
      }
      .narrow
