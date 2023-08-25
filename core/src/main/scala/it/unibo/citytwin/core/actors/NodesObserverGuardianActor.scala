package it.unibo.citytwin.core.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.citytwin.core.actors.MainstayActor.mainstayService
import it.unibo.citytwin.core.actors.ResourceActor.resourceService

/**
  * This object defines the behavior of Nodes Observer Guardian Actor that subscribes to Mainstay Actor and Resource Actor events using the Receptionist pattern.
  */
object NodesObserverGuardianActor:

  /**
   * Generates new Nodes Observer Guardian Actor.
   * @param mainstay the Mainstay Actor that will receive the Mainstay Actor and Resource Actor events.
   * @return the behavior of Nodes Observer Guardian Actor.
   */
  def apply(mainstay: ActorRef[MainstayActorCommand]): Behavior[Nothing] =
    Behaviors
      .setup[Receptionist.Listing] { ctx =>
        ctx.log.debug("Nodes observer guardian started")
        val nodesObserverActor = ctx.spawnAnonymous(NodesObserverActor(mainstay))
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
            nodesObserverActor ! UpdateMainstayNodesState(listings)
            Behaviors.same
          }
          case resourceService.Listing(listings) => {
            ctx.log.debug("Received mainstayService")
            nodesObserverActor ! UpdateResourceNodesState(listings)
            Behaviors.same
          }
          case _ => Behaviors.stopped
        }
      }
      .narrow
