package it.unibo.citytwin.rivermonitor.actors.view

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import it.unibo.citytwin.core.actors.MainstayActor.mainstayService
import it.unibo.citytwin.core.actors.SetMainstayActorsToResourceActor

object ViewGuardianActor:
  def apply(viewName: String,
            width: Int,
            height: Int): Behavior[Nothing] = 
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val viewActor = ctx.spawnAnonymous(ViewActor(viewName, width, height))
      val resourceViewActor = ctx.spawnAnonymous(ResourceViewActor(viewActor))
      viewActor ! SetResourceActor(resourceViewActor)
      
      ctx.system.receptionist ! Receptionist.subscribe(mainstayService, ctx.self)

      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case mainstayService.Listing(listings) =>{
          ctx.log.debug("Received mainstayService")
          //tell all mainstayActors to resourceViewActor
          resourceViewActor ! SetMainstayActorsToResourceActor(listings)
          Behaviors.same
        }
        case _ => {
          ctx.log.debug(s"Unexpected message. The actor is being stopped")
          Behaviors.stopped
        }
      }
    }.narrow

