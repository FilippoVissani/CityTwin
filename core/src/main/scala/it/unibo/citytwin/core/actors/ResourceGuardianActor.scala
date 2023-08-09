package it.unibo.citytwin.core.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.MainstayActor.mainstayService

object ResourceGuardianActor:
  def apply(resourceActor: ActorRef[ResourceActorCommand]): Behavior[Nothing] =
    Behaviors
      .setup[Receptionist.Listing] { ctx =>
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
