package it.unibo.citytwin.core.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.MainstayActor.mainstayService

/** This object defines the behavior of Resource Guardian Actor that subscribes to Mainstay Actor
  * events using the Receptionist pattern.
  */
object ResourceGuardianActor:
  /** Generates new Resource Guardian Actor.
    * @param resourceActor
    *   the Resource Actor that will receive the Mainstay Actor events.
    * @return
    *   the behavior of Resource Guardian Actor.
    */
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
