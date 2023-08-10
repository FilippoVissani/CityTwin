package it.unibo.citytwin.rivermonitor.actors.view

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import it.unibo.citytwin.core.actors.ResourceActor.resourceService
import it.unibo.citytwin.core.actors.*
import it.unibo.citytwin.core.model.Resource
import it.unibo.citytwin.core.model.ResourceType.{Act, Sense}
import it.unibo.citytwin.rivermonitor.model.RiverMonitorState.{RiverMonitorState, Safe, Evacuating, Warned}
import it.unibo.citytwin.rivermonitor.actors.view.UpdateRiverMonitorState
import scala.util.Success
import scala.concurrent.duration.DurationInt

object ResourceViewActor :
  def apply(viewActor: ActorRef[ViewActorCommand],
            mainstayActors: Set[ActorRef[MainstayActorCommand]] = Set()): Behavior[ResourceActorCommand] =
    Behaviors.setup[ResourceActorCommand] { ctx =>
      Behaviors.withTimers { timers =>
        ctx.system.receptionist ! Receptionist.Register(resourceService, ctx.self)
        //TODO: take resourcesToCheck from view
        val resourcesToCheck = Set[String]("riverMonitor1")
        timers.startTimerAtFixedRate(AskResourcesToMainstay(resourcesToCheck), 1.seconds)
        ResourceViewActorLogic(ctx, viewActor, mainstayActors)
      }
    }

  private def ResourceViewActorLogic(ctx: ActorContext[ResourceActorCommand],
                                     viewActor: ActorRef[ViewActorCommand],
                                     mainstayActors: Set[ActorRef[MainstayActorCommand]]): Behavior[ResourceActorCommand] =
    implicit val timeout: Timeout = 3.seconds
    Behaviors.receiveMessage {
      case AskResourcesToMainstay(resourcesNames) => {
        ctx.log.debug("Received AskResourcesToMainstay")
        if mainstayActors.nonEmpty then
          ctx.ask(mainstayActors.head, ref => AskResourcesState(ref, resourcesNames)){
            case Success(ResourceStatesResponse(resources: Set[Resource])) => AdaptedResourcesStateResponse(resources)
            case _ => {
              ctx.log.debug("Resources not received from Mainstay actor. Mainstay actor is unreachable.")
              AdaptedResourcesStateResponse(Set())
            }
          }
        Behaviors.same
      }
      case AdaptedResourcesStateResponse(resources) => {
        ctx.log.debug("Received AdaptedResourcesStateResponse")
        if resources.nonEmpty then
          resources.filter(resource => resource.state.nonEmpty).foreach(resource => {
            viewActor ! UpdateRiverMonitorState(resource.state.get.asInstanceOf[String])
          })
        Behaviors.same
      }
      case SetMainstayActorsToResourceActor(mainstayActors) => {
        ctx.log.debug("Received SetMainstayActorsToResourceActor")
        ResourceViewActorLogic(ctx, viewActor, mainstayActors)
      }
      case ResourceChanged(resource) => {
        ctx.log.debug("Received ResourceChanged")
        //ricevuto dalla view quando viene premuto un tasto. è una risorsa che leggerà il resourceRiverMonitorActor
        mainstayActors.foreach(mainstay => mainstay ! UpdateResources(Map(ctx.self -> resource).toSet))
        Behaviors.same
      }
      case _ => {
        ctx.log.debug(s"Unexpected message. The actor is being stopped")
        Behaviors.stopped
      }
    }